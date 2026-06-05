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
    ORDER_CANCEL_REQUIRES_REFUND("ORDER-004", "Paid order cancellation requires refund flow", HttpStatus.BAD_REQUEST),
    ORDER_INVALID_STATUS_TRANSITION("ORDER-005", "Invalid order status transition", HttpStatus.BAD_REQUEST),
    ORDER_REFUND_NOT_ALLOWED("ORDER-006", "Order is not eligible for manual refund", HttpStatus.BAD_REQUEST),
    ORDER_REFUND_ALREADY_PROCESSED("ORDER-007", "Order refund has already been processed", HttpStatus.BAD_REQUEST),
    ORDER_REFUND_AMOUNT_INVALID("ORDER-008", "Refund amount is invalid", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
