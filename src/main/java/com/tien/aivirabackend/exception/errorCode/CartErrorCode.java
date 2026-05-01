package com.tien.aivirabackend.exception.errorCode;

import org.springframework.http.HttpStatus;

import com.tien.aivirabackend.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CartErrorCode implements ErrorCode {
    CART_ITEM_NOT_FOUND("CART-001", "Cart item not found", HttpStatus.NOT_FOUND),
    CART_ITEM_REQUIRED("CART-002", "At least one cart item is required", HttpStatus.BAD_REQUEST),
    CART_INVALID_QUANTITY("CART-003", "Cart item quantity is invalid", HttpStatus.BAD_REQUEST),
    CART_PRODUCT_NOT_AVAILABLE("CART-004", "Product is not available for cart", HttpStatus.BAD_REQUEST),
    CART_STOCK_NOT_ENOUGH("CART-005", "Product stock is not enough", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
