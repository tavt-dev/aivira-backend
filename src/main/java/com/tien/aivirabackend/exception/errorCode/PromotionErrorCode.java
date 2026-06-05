package com.tien.aivirabackend.exception.errorCode;

import org.springframework.http.HttpStatus;

import com.tien.aivirabackend.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PromotionErrorCode implements ErrorCode {
    PROMOTION_NOT_FOUND("PROMOTION-001", "Promotion not found", HttpStatus.NOT_FOUND),
    PROMOTION_NAME_ALREADY_EXISTS("PROMOTION-002", "Promotion name already exists", HttpStatus.CONFLICT),
    PROMOTION_INVALID_DATE_RANGE("PROMOTION-003", "Promotion date range is invalid", HttpStatus.BAD_REQUEST),
    PROMOTION_INVALID_TARGET("PROMOTION-004", "Promotion target is invalid", HttpStatus.BAD_REQUEST),
    DISCOUNT_INVALID_VALUE("PROMOTION-005", "Discount value is invalid", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
