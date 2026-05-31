package com.tien.aivirabackend.service.commerce;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tien.aivirabackend.constant.ProductStatus;
import com.tien.aivirabackend.domain.dto.request.CartItemRequest;
import com.tien.aivirabackend.domain.dto.request.CartItemUpdateRequest;
import com.tien.aivirabackend.domain.dto.response.CartResponse;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.entity.catalog.ProductVariation;
import com.tien.aivirabackend.domain.entity.transaction.Cart;
import com.tien.aivirabackend.domain.entity.transaction.CartItem;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.mapper.CommerceMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.CartErrorCode;
import com.tien.aivirabackend.repository.CartItemRepository;
import com.tien.aivirabackend.repository.CartRepository;
import com.tien.aivirabackend.repository.ProductVariationRepository;
import com.tien.aivirabackend.service.auth.CurrentUserService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CartServiceImpl implements CartService {
    CartRepository cartRepository;
    CartItemRepository cartItemRepository;
    ProductVariationRepository variationRepository;
    CurrentUserService currentUserService;
    CommerceMapper commerceMapper;

    @Override
    @Transactional
    public CartResponse getMyCart() {
        User user = currentUserService.getCurrentUser();
        return commerceMapper.toCartResponse(getOrCreateCart(user));
    }

    @Override
    @Transactional
    public CartResponse addItem(CartItemRequest request) {
        User user = currentUserService.getCurrentUser();
        Cart cart = getOrCreateCart(user);
        ProductVariation variation = variationRepository
                .findById(request.getProductVariationId())
                .orElseThrow(() -> new AppException(CartErrorCode.CART_PRODUCT_NOT_AVAILABLE));
        validateAvailable(variation, request.getQuantity());
        cartItemRepository
                .findByCartIdAndProductVariationId(cart.getId(), variation.getId())
                .ifPresentOrElse(
                        item -> {
                            int newQuantity = item.getQuantity() + request.getQuantity();
                            validateAvailable(variation, newQuantity);
                            item.setQuantity(newQuantity);
                            cartItemRepository.save(item);
                        },
                        () -> {
                            CartItem created = CartItem.builder()
                                    .cart(cart)
                                    .productVariation(variation)
                                    .quantity(request.getQuantity())
                                    .build();
                            cart.getItems().add(cartItemRepository.save(created));
                        });
        return commerceMapper.toCartResponse(getOrCreateCart(user));
    }

    @Override
    @Transactional
    public CartResponse updateItem(Long cartItemId, CartItemUpdateRequest request) {
        User user = currentUserService.getCurrentUser();
        CartItem item = cartItemRepository
                .findByIdAndCartUserIdAndCartActiveTrue(cartItemId, user.getId())
                .orElseThrow(() -> new AppException(CartErrorCode.CART_ITEM_NOT_FOUND));
        validateAvailable(item.getProductVariation(), request.getQuantity());
        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);
        return commerceMapper.toCartResponse(getOrCreateCart(user));
    }

    @Override
    @Transactional
    public void removeItem(Long cartItemId) {
        User user = currentUserService.getCurrentUser();
        CartItem item = cartItemRepository
                .findByIdAndCartUserIdAndCartActiveTrue(cartItemId, user.getId())
                .orElseThrow(() -> new AppException(CartErrorCode.CART_ITEM_NOT_FOUND));
        cartItemRepository.delete(item);
    }

    @Override
    @Transactional
    public void clear() {
        User user = currentUserService.getCurrentUser();
        Cart cart = getOrCreateCart(user);
        cartItemRepository.deleteByCartId(cart.getId());
    }

    private Cart getOrCreateCart(User user) {
        return cartRepository
                .findByUserIdAndActiveTrue(user.getId())
                .orElseGet(() -> cartRepository.save(
                        Cart.builder().user(user).active(true).build()));
    }

    private void validateAvailable(ProductVariation variation, int quantity) {
        Product product = variation.getProduct();
        if (!Boolean.TRUE.equals(variation.getActive())
                || !Boolean.TRUE.equals(product.getActive())
                || product.getStatus() != ProductStatus.ACTIVE) {
            throw new AppException(CartErrorCode.CART_PRODUCT_NOT_AVAILABLE);
        }
        if (quantity < 1) {
            throw new AppException(CartErrorCode.CART_INVALID_QUANTITY);
        }
        if (variation.getStockQuantity() < quantity) {
            throw new AppException(CartErrorCode.CART_STOCK_NOT_ENOUGH);
        }
    }
}
