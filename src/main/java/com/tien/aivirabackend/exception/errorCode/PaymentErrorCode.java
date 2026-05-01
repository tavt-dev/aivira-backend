package com.tien.aivirabackend.exception.errorCode;

import org.springframework.http.HttpStatus;

import com.tien.aivirabackend.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {
    PAYMENT_GROUP_NOT_FOUND("PAYMENT-001", "Payment group not found", HttpStatus.NOT_FOUND),
    PAYMENT_NOT_FOUND("PAYMENT-002", "Payment not found", HttpStatus.NOT_FOUND),
    PAYMENT_INVALID_SIGNATURE("PAYMENT-003", "Payment callback signature is invalid", HttpStatus.BAD_REQUEST),
    PAYMENT_AMOUNT_MISMATCH("PAYMENT-004", "Payment callback amount does not match", HttpStatus.BAD_REQUEST),
    PAYMENT_INVALID_STATUS("PAYMENT-005", "Payment status does not allow this action", HttpStatus.BAD_REQUEST),
    PAYMENT_PROVIDER_ERROR("PAYMENT-006", "Payment provider request failed", HttpStatus.BAD_GATEWAY);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
