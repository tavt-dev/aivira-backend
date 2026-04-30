package com.tien.aivirabackend.exception.errorCode;

import org.springframework.http.HttpStatus;

import com.tien.aivirabackend.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductErrorCode implements ErrorCode {
    PRODUCT_NOT_FOUND("E10100", "Product not found.", HttpStatus.NOT_FOUND),
    PRODUCT_SKU_ALREADY_EXISTS("E10101", "Product SKU already exists.", HttpStatus.CONFLICT),
    PRODUCT_SLUG_ALREADY_EXISTS("E10102", "Product slug already exists.", HttpStatus.CONFLICT),
    PRODUCT_NOT_OWNER("E10103", "Current seller does not own this product.", HttpStatus.FORBIDDEN),
    PRODUCT_INVALID_STATUS_TRANSITION("E10104", "Invalid product status transition.", HttpStatus.BAD_REQUEST),
    PRODUCT_VARIATION_REQUIRED("E10105", "Product must have at least one active variation.", HttpStatus.BAD_REQUEST),
    PRODUCT_VARIATION_NOT_FOUND("E10106", "Product variation not found.", HttpStatus.NOT_FOUND),
    PRODUCT_VARIATION_SKU_ALREADY_EXISTS("E10107", "Product variation SKU already exists.", HttpStatus.CONFLICT),
    PRODUCT_MEDIA_NOT_FOUND("E10108", "Product media not found.", HttpStatus.NOT_FOUND),
    PRODUCT_MEDIA_UPLOAD_FAILED("E10109", "Product media upload failed.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
