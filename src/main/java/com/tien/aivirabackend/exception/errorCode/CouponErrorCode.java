package com.tien.aivirabackend.exception.errorCode;

import org.springframework.http.HttpStatus;

import com.tien.aivirabackend.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CouponErrorCode implements ErrorCode {
    COUPON_NOT_FOUND("COUPON-001", "Coupon not found", HttpStatus.NOT_FOUND),
    COUPON_CODE_ALREADY_EXISTS("COUPON-002", "Coupon code already exists", HttpStatus.CONFLICT),
    COUPON_INVALID("COUPON-003", "Coupon is invalid", HttpStatus.BAD_REQUEST),
    COUPON_EXPIRED("COUPON-004", "Coupon is expired or not started", HttpStatus.BAD_REQUEST),
    COUPON_USAGE_LIMIT_EXCEEDED("COUPON-005", "Coupon usage limit exceeded", HttpStatus.BAD_REQUEST),
    COUPON_MIN_ORDER_NOT_MET("COUPON-006", "Coupon minimum order amount is not met", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
