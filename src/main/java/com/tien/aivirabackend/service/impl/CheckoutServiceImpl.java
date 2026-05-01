package com.tien.aivirabackend.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.tien.aivirabackend.config.properties.PaymentProperties;
import com.tien.aivirabackend.constant.OrderStatus;
import com.tien.aivirabackend.constant.PaymentMethod;
import com.tien.aivirabackend.constant.PaymentStatus;
import com.tien.aivirabackend.constant.ProductStatus;
import com.tien.aivirabackend.constant.ShopStatus;
import com.tien.aivirabackend.domain.dto.request.CheckoutRequest;
import com.tien.aivirabackend.domain.dto.response.CheckoutResponse;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.entity.catalog.ProductVariation;
import com.tien.aivirabackend.domain.entity.marketplace.Shop;
import com.tien.aivirabackend.domain.entity.transaction.CartItem;
import com.tien.aivirabackend.domain.entity.transaction.Order;
import com.tien.aivirabackend.domain.entity.transaction.OrderItem;
import com.tien.aivirabackend.domain.entity.transaction.payment.Payment;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentGroup;
import com.tien.aivirabackend.domain.entity.user.Address;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.mapper.CommerceMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.AddressErrorCode;
import com.tien.aivirabackend.exception.errorCode.CartErrorCode;
import com.tien.aivirabackend.exception.errorCode.CheckoutErrorCode;
import com.tien.aivirabackend.repository.*;
import com.tien.aivirabackend.service.CheckoutService;
import com.tien.aivirabackend.service.CurrentUserService;
import com.tien.aivirabackend.service.payment.PaymentProviderClient;
import com.tien.aivirabackend.service.payment.PaymentProviderResult;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CheckoutServiceImpl implements CheckoutService {
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    CartItemRepository cartItemRepository;
    AddressRepository addressRepository;
    ProductVariationRepository variationRepository;
    OrderRepository orderRepository;
    PaymentGroupRepository paymentGroupRepository;
    PaymentProperties paymentProperties;
    CurrentUserService currentUserService;
    CommerceMapper commerceMapper;
    List<PaymentProviderClient> paymentProviderClients;

    @Override
    @Transactional
    public CheckoutResponse checkout(CheckoutRequest request) {
        User user = currentUserService.getCurrentUser();
        validatePaymentMethod(request.getPaymentMethod());
        Address address = addressRepository
                .findByIdAndUserIdAndActiveTrue(request.getAddressId(), user.getId())
                .orElseThrow(() -> new AppException(AddressErrorCode.ADDRESS_NOT_FOUND));

        List<Long> selectedIds =
                request.getCartItemIds().stream().distinct().sorted().toList();
        if (selectedIds.isEmpty()) {
            throw new AppException(CheckoutErrorCode.CHECKOUT_EMPTY_ITEMS);
        }
        List<CartItem> cartItems =
                cartItemRepository.findByIdInAndCartUserIdAndCartActiveTrue(selectedIds, user.getId());
        if (cartItems.size() != selectedIds.size()) {
            throw new AppException(CheckoutErrorCode.CHECKOUT_CART_ITEM_MISMATCH);
        }

        Map<Long, ProductVariation> lockedVariations = variationRepository
                .findAllByIdInForUpdate(cartItems.stream()
                        .map(item -> item.getProductVariation().getId())
                        .distinct()
                        .sorted()
                        .toList())
                .stream()
                .collect(Collectors.toMap(ProductVariation::getId, Function.identity()));

        cartItems.forEach(item -> validateCheckoutItem(
                item, lockedVariations.get(item.getProductVariation().getId())));

        PaymentGroup paymentGroup = PaymentGroup.builder()
                .paymentCode(generatePaymentCode())
                .user(user)
                .method(request.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .amount(ZERO)
                .expiresAt(Instant.now().plusSeconds(paymentProperties.getPendingTtlMinutes() * 60))
                .build();

        Map<Long, List<CartItem>> itemsByShop = cartItems.stream()
                .collect(Collectors.groupingBy(item -> lockedVariations
                        .get(item.getProductVariation().getId())
                        .getProduct()
                        .getShop()
                        .getId()));

        List<Order> orders = new ArrayList<>();
        BigDecimal totalAmount = ZERO;
        for (List<CartItem> shopItems : itemsByShop.values()) {
            Order order = buildOrder(user, address, request, shopItems, lockedVariations);
            orders.add(order);
            totalAmount = totalAmount.add(order.getTotalAmount());
        }
        paymentGroup.setAmount(totalAmount);

        PaymentGroup savedPaymentGroup = paymentGroupRepository.save(paymentGroup);
        OrderStatus initialOrderStatus = request.getPaymentMethod() == PaymentMethod.COD
                ? OrderStatus.PENDING_CONFIRMATION
                : OrderStatus.PENDING_PAYMENT;
        for (Order order : orders) {
            order.setOrderStatus(initialOrderStatus);
            Payment payment = Payment.builder()
                    .order(order)
                    .paymentGroup(savedPaymentGroup)
                    .method(request.getPaymentMethod())
                    .status(PaymentStatus.PENDING)
                    .amount(order.getTotalAmount())
                    .build();
            order.getPayments().add(payment);
            savedPaymentGroup.getPayments().add(payment);
        }

        deductStock(cartItems, lockedVariations);
        List<Order> savedOrders = orderRepository.saveAll(orders);
        PaymentProviderResult providerResult =
                provider(request.getPaymentMethod()).createPayment(savedPaymentGroup);
        applyProviderResult(savedPaymentGroup, providerResult);
        paymentGroupRepository.save(savedPaymentGroup);

        if (request.getPaymentMethod() == PaymentMethod.COD) {
            cartItemRepository.deleteAll(cartItems);
        }

        return CheckoutResponse.builder()
                .paymentGroupCode(savedPaymentGroup.getPaymentCode())
                .paymentMethod(savedPaymentGroup.getMethod())
                .paymentStatus(savedPaymentGroup.getStatus())
                .totalAmount(savedPaymentGroup.getAmount())
                .paymentUrl(savedPaymentGroup.getPaymentUrl())
                .deeplink(savedPaymentGroup.getDeeplink())
                .qrCodeUrl(savedPaymentGroup.getQrCodeUrl())
                .expiresAt(savedPaymentGroup.getExpiresAt())
                .orders(savedOrders.stream()
                        .map(commerceMapper::toOrderResponse)
                        .toList())
                .build();
    }

    private Order buildOrder(
            User user,
            Address address,
            CheckoutRequest request,
            List<CartItem> cartItems,
            Map<Long, ProductVariation> lockedVariations) {
        ProductVariation firstVariation =
                lockedVariations.get(cartItems.getFirst().getProductVariation().getId());
        Shop shop = firstVariation.getProduct().getShop();
        Order order = Order.builder()
                .orderCode(generateOrderCode())
                .user(user)
                .shop(shop)
                .shippingAddress(address)
                .shippingRecipientName(address.getRecipientName())
                .shippingPhoneNumber(address.getPhoneNumber())
                .shippingAddressLine(address.getAddressLine())
                .shippingWard(address.getWard())
                .shippingDistrict(address.getDistrict())
                .shippingCity(address.getCity())
                .shippingFee(ZERO)
                .discountAmount(ZERO)
                .notes(trimToNull(request.getNotes()))
                .build();
        BigDecimal subtotal = ZERO;
        for (CartItem cartItem : cartItems) {
            ProductVariation variation =
                    lockedVariations.get(cartItem.getProductVariation().getId());
            Product product = variation.getProduct();
            BigDecimal additionalPrice = nullToZero(variation.getAdditionalPrice());
            BigDecimal finalPrice = product.getPrice().add(additionalPrice);
            subtotal = subtotal.add(finalPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productId(product.getId())
                    .productVariationId(variation.getId())
                    .productName(product.getProductName())
                    .sku(variation.getSku())
                    .variationColor(variation.getColor())
                    .variationSize(variation.getSize())
                    .thumbnailUrl(
                            StringUtils.hasText(variation.getImageUrl())
                                    ? variation.getImageUrl()
                                    : product.getThumbnailUrl())
                    .basePrice(product.getPrice())
                    .additionalPrice(additionalPrice)
                    .discountAmount(ZERO)
                    .finalPrice(finalPrice)
                    .quantity(cartItem.getQuantity())
                    .build();
            order.getItems().add(orderItem);
        }
        order.setSubtotal(subtotal);
        order.setTotalAmount(subtotal);
        return order;
    }

    private void validateCheckoutItem(CartItem item, ProductVariation variation) {
        if (variation == null) {
            throw new AppException(CheckoutErrorCode.CHECKOUT_CART_ITEM_MISMATCH);
        }
        Product product = variation.getProduct();
        if (!Boolean.TRUE.equals(variation.getActive())
                || !Boolean.TRUE.equals(product.getActive())
                || product.getStatus() != ProductStatus.ACTIVE
                || product.getShop() == null
                || product.getShop().getStatus() != ShopStatus.APPROVED) {
            throw new AppException(CartErrorCode.CART_PRODUCT_NOT_AVAILABLE);
        }
        if (variation.getStockQuantity() < item.getQuantity()) {
            throw new AppException(CartErrorCode.CART_STOCK_NOT_ENOUGH);
        }
    }

    private void deductStock(List<CartItem> cartItems, Map<Long, ProductVariation> lockedVariations) {
        for (CartItem item : cartItems) {
            ProductVariation variation =
                    lockedVariations.get(item.getProductVariation().getId());
            Product product = variation.getProduct();
            variation.setStockQuantity(variation.getStockQuantity() - item.getQuantity());
            product.setStockQuantity(Math.max(0, product.getStockQuantity() - item.getQuantity()));
        }
    }

    private void applyProviderResult(PaymentGroup paymentGroup, PaymentProviderResult result) {
        paymentGroup.setProviderTxnRef(result.providerTxnRef());
        paymentGroup.setProviderTransactionId(result.providerTransactionId());
        paymentGroup.setPaymentUrl(result.paymentUrl());
        paymentGroup.setDeeplink(result.deeplink());
        paymentGroup.setQrCodeUrl(result.qrCodeUrl());
        paymentGroup.setRawResponse(result.rawResponse());
    }

    private PaymentProviderClient provider(PaymentMethod method) {
        return paymentProviderClients.stream()
                .filter(client -> client.method() == method)
                .findFirst()
                .orElseThrow(() -> new AppException(CheckoutErrorCode.CHECKOUT_PAYMENT_METHOD_UNSUPPORTED));
    }

    private void validatePaymentMethod(PaymentMethod method) {
        if (method == null
                || (method != PaymentMethod.COD && method != PaymentMethod.VNPAY && method != PaymentMethod.MOMO)) {
            throw new AppException(CheckoutErrorCode.CHECKOUT_PAYMENT_METHOD_UNSUPPORTED);
        }
    }

    private String generatePaymentCode() {
        String code;
        do {
            code = "PAY" + System.currentTimeMillis()
                    + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (paymentGroupRepository.existsByPaymentCode(code));
        return code;
    }

    private String generateOrderCode() {
        String code;
        do {
            code = "ORD" + System.currentTimeMillis()
                    + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (orderRepository.existsByOrderCode(code));
        return code;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
