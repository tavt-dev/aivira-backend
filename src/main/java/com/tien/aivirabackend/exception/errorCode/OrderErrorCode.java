package com.tien.aivirabackend.exception.errorCode;

import org.springframework.http.HttpStatus;

import com.tien.aivirabackend.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderErrorCode implements ErrorCode {
    ORDER_NOT_FOUND("ORDER-001", "Order not found", HttpStatus.NOT_FOUND),
    ORDER_CANCEL_NOT_ALLOWED("ORDER-002", "Order status does not allow cancellation", HttpStatus.BAD_REQUEST),
    ORDER_SHARED_PAYMENT_GROUP_CANCEL_NOT_SUPPORTED(
            "ORDER-003", "Cannot cancel a single order in a shared pending payment group", HttpStatus.BAD_REQUEST),
    ORDER_CANCEL_REQUIRES_REFUND("ORDER-004", "Paid order cancellation requires refund flow", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
