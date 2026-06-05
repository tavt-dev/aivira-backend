package com.tien.aivirabackend.exception.errorCode;

import org.springframework.http.HttpStatus;

import com.tien.aivirabackend.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements ErrorCode {
    REVIEW_NOT_FOUND("REVIEW-001", "Review not found", HttpStatus.NOT_FOUND),
    REVIEW_NOT_ALLOWED("REVIEW-002", "Review action is not allowed", HttpStatus.BAD_REQUEST),
    REVIEW_ALREADY_EXISTS("REVIEW-003", "Review already exists for this order item", HttpStatus.CONFLICT),
    REVIEW_ORDER_NOT_COMPLETED("REVIEW-004", "Order must be completed before review", HttpStatus.BAD_REQUEST),
    REVIEW_DELETED("REVIEW-005", "Review has been deleted", HttpStatus.BAD_REQUEST),
    REVIEW_INVALID_IMAGE("REVIEW-006", "Review image is invalid", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
