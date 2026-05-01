package com.tien.aivirabackend.exception.errorCode;

import org.springframework.http.HttpStatus;

import com.tien.aivirabackend.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CheckoutErrorCode implements ErrorCode {
    CHECKOUT_EMPTY_ITEMS("CHECKOUT-001", "Checkout item list must not be empty", HttpStatus.BAD_REQUEST),
    CHECKOUT_CART_ITEM_MISMATCH(
            "CHECKOUT-002", "Some checkout items do not belong to current cart", HttpStatus.BAD_REQUEST),
    CHECKOUT_PAYMENT_METHOD_UNSUPPORTED("CHECKOUT-003", "Payment method is not supported", HttpStatus.BAD_REQUEST),
    CHECKOUT_PAYMENT_PROVIDER_DISABLED("CHECKOUT-004", "Payment provider is disabled", HttpStatus.BAD_REQUEST),
    CHECKOUT_PAYMENT_PROVIDER_ERROR("CHECKOUT-005", "Payment provider request failed", HttpStatus.BAD_GATEWAY);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
