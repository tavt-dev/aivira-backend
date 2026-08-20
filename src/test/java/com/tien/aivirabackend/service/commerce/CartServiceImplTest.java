package com.tien.aivirabackend.service.commerce;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tien.aivirabackend.constant.ProductStatus;
import com.tien.aivirabackend.domain.dto.request.CartItemRequest;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.entity.catalog.ProductVariation;
import com.tien.aivirabackend.domain.entity.transaction.Cart;
import com.tien.aivirabackend.domain.entity.transaction.CartItem;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.mapper.CommerceMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.repository.CartItemRepository;
import com.tien.aivirabackend.repository.CartRepository;
import com.tien.aivirabackend.repository.ProductVariationRepository;
import com.tien.aivirabackend.service.auth.CurrentUserService;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {
    @Mock
    CartRepository cartRepository;

    @Mock
    CartItemRepository cartItemRepository;

    @Mock
    ProductVariationRepository variationRepository;

    @Mock
    CurrentUserService currentUserService;

    CommerceMapper commerceMapper = new CommerceMapper();

    @Test
    void addItem_shouldMergeDuplicateVariationQuantity() {
        User user = User.builder().id("user-1").build();
        Cart cart = Cart.builder().id(1L).user(user).active(true).items(new ArrayList<>()).build();
        ProductVariation variation = activeVariation(10);
        CartItem existing = CartItem.builder().id(1L).cart(cart).productVariation(variation).quantity(2).build();
        cart.getItems().add(existing);
        CartServiceImpl service = new CartServiceImpl(cartRepository, cartItemRepository, variationRepository,
                currentUserService, commerceMapper);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(cartRepository.findByUserIdAndActiveTrue("user-1")).thenReturn(Optional.of(cart));
        when(variationRepository.findById(1L)).thenReturn(Optional.of(variation));
        when(cartItemRepository.findByCartIdAndProductVariationId(1L, 1L)).thenReturn(Optional.of(existing));

        service.addItem(CartItemRequest.builder().productVariationId(1L).quantity(3).build());

        verify(cartItemRepository).save(existing);
        org.assertj.core.api.Assertions.assertThat(existing.getQuantity()).isEqualTo(5);
    }

    @Test
    void addItem_shouldRejectInsufficientStock() {
        User user = User.builder().id("user-1").build();
        Cart cart = Cart.builder().id(1L).user(user).active(true).items(new ArrayList<>()).build();
        ProductVariation variation = activeVariation(1);
        CartServiceImpl service = new CartServiceImpl(cartRepository, cartItemRepository, variationRepository,
                currentUserService, commerceMapper);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(cartRepository.findByUserIdAndActiveTrue("user-1")).thenReturn(Optional.of(cart));
        when(variationRepository.findById(1L)).thenReturn(Optional.of(variation));

        assertThatThrownBy(() -> service.addItem(CartItemRequest.builder().productVariationId(1L).quantity(2).build()))
                .isInstanceOf(AppException.class);
        verify(cartItemRepository, never()).save(any());
    }

    private ProductVariation activeVariation(int stock) {
        Product product = Product.builder().id(1L).productName("Product").slug("product")
                .price(java.math.BigDecimal.TEN).active(true).status(ProductStatus.ACTIVE).build();
        return ProductVariation.builder().id(1L).product(product).sku("SKU-1").color("Black").size("M")
                .stockQuantity(stock).active(true).build();
    }
}
