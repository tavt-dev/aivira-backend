package com.tien.aivirabackend.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.domain.dto.request.CartItemRequest;
import com.tien.aivirabackend.domain.dto.request.CartItemUpdateRequest;
import com.tien.aivirabackend.domain.dto.response.CartResponse;
import com.tien.aivirabackend.service.CartService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/cart")
@Tag(name = "Cart")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CartController {
    CartService cartService;

    @GetMapping
    @Operation(summary = "Get my active cart")
    @PreAuthorize("@authorizationService.hasPermission('CART_READ_SELF')")
    public ResponseEntity<ApiResponse<CartResponse>> getMyCart() {
        return ResponseEntity.ok(ApiResponse.success("Get cart successful", cartService.getMyCart()));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart")
    @PreAuthorize("@authorizationService.hasPermission('CART_UPDATE_SELF')")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(@Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Add cart item successful", cartService.addItem(request)));
    }

    @PutMapping("/items/{cartItemId}")
    @Operation(summary = "Update cart item quantity")
    @PreAuthorize("@authorizationService.hasPermission('CART_UPDATE_SELF')")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            @PathVariable Long cartItemId, @Valid @RequestBody CartItemUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Update cart item successful", cartService.updateItem(cartItemId, request)));
    }

    @DeleteMapping("/items/{cartItemId}")
    @Operation(summary = "Remove cart item")
    @PreAuthorize("@authorizationService.hasPermission('CART_UPDATE_SELF')")
    public ResponseEntity<ApiResponse<Void>> removeItem(@PathVariable Long cartItemId) {
        cartService.removeItem(cartItemId);
        return ResponseEntity.ok(ApiResponse.success("Remove cart item successful", null));
    }

    @DeleteMapping("/items")
    @Operation(summary = "Clear cart")
    @PreAuthorize("@authorizationService.hasPermission('CART_CLEAR_SELF')")
    public ResponseEntity<ApiResponse<Void>> clear() {
        cartService.clear();
        return ResponseEntity.ok(ApiResponse.success("Clear cart successful", null));
    }
}
