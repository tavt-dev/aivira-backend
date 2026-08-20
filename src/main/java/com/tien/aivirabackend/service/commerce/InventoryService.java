package com.tien.aivirabackend.service.commerce;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.tien.aivirabackend.constant.ProductStatus;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.entity.catalog.ProductVariation;
import com.tien.aivirabackend.domain.entity.transaction.CartItem;
import com.tien.aivirabackend.domain.entity.transaction.Order;
import com.tien.aivirabackend.domain.entity.transaction.OrderItem;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.CartErrorCode;
import com.tien.aivirabackend.exception.errorCode.CheckoutErrorCode;
import com.tien.aivirabackend.repository.ProductVariationRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InventoryService {
    ProductVariationRepository variationRepository;

    public Map<Long, ProductVariation> lockVariationsForCartItems(List<CartItem> cartItems) {
        List<Long> variationIds = cartItems.stream().map(item -> item.getProductVariation().getId()).distinct().sorted()
                .toList();
        return lockVariations(variationIds);
    }

    public void validateCheckoutItems(List<CartItem> cartItems, Map<Long, ProductVariation> lockedVariations) {
        cartItems.forEach(item -> validateCheckoutItem(item, lockedVariations.get(item.getProductVariation().getId())));
    }

    public void deductCartItems(List<CartItem> cartItems, Map<Long, ProductVariation> lockedVariations) {
        for (CartItem item : cartItems) {
            ProductVariation variation = lockedVariations.get(item.getProductVariation().getId());
            Product product = variation.getProduct();
            variation.setStockQuantity(variation.getStockQuantity() - item.getQuantity());
            product.setStockQuantity(Math.max(0, product.getStockQuantity() - item.getQuantity()));
        }
    }

    public void deductStockForOrders(List<Order> orders) {
        Map<Long, Integer> quantities = orderVariationQuantities(orders);
        Map<Long, ProductVariation> variations = lockVariations(quantities.keySet());
        quantities.forEach((variationId, quantity) -> {
            ProductVariation variation = variations.get(variationId);
            if (variation == null || variation.getStockQuantity() < quantity) {
                throw new AppException(CartErrorCode.CART_STOCK_NOT_ENOUGH);
            }
            Product product = variation.getProduct();
            variation.setStockQuantity(variation.getStockQuantity() - quantity);
            product.setStockQuantity(Math.max(0, product.getStockQuantity() - quantity));
        });
    }

    public void restoreStockForOrders(List<Order> orders) {
        Map<Long, Integer> quantities = orderVariationQuantities(orders);
        Map<Long, ProductVariation> variations = lockVariations(quantities.keySet());
        quantities.forEach((variationId, quantity) -> {
            ProductVariation variation = variations.get(variationId);
            if (variation != null) {
                Product product = variation.getProduct();
                variation.setStockQuantity(variation.getStockQuantity() + quantity);
                product.setStockQuantity(product.getStockQuantity() + quantity);
            }
        });
    }

    public Map<Long, Integer> orderVariationQuantities(List<Order> orders) {
        return orders.stream().flatMap(order -> order.getItems().stream())
                .filter(item -> item.getProductVariationId() != null).collect(Collectors
                        .toMap(OrderItem::getProductVariationId, OrderItem::getQuantity, Integer::sum, TreeMap::new));
    }

    private Map<Long, ProductVariation> lockVariations(Collection<Long> variationIds) {
        if (variationIds.isEmpty()) {
            return Map.of();
        }
        return variationRepository.findAllByIdInForUpdate(variationIds).stream()
                .collect(Collectors.toMap(ProductVariation::getId, Function.identity()));
    }

    private void validateCheckoutItem(CartItem item, ProductVariation variation) {
        if (variation == null) {
            throw new AppException(CheckoutErrorCode.CHECKOUT_CART_ITEM_MISMATCH);
        }
        Product product = variation.getProduct();
        if (!Boolean.TRUE.equals(variation.getActive()) || !Boolean.TRUE.equals(product.getActive())
                || product.getStatus() != ProductStatus.ACTIVE) {
            throw new AppException(CartErrorCode.CART_PRODUCT_NOT_AVAILABLE);
        }
        if (variation.getStockQuantity() < item.getQuantity()) {
            throw new AppException(CartErrorCode.CART_STOCK_NOT_ENOUGH);
        }
    }
}
