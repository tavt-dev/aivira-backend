package com.tien.aivirabackend.exception.errorCode;

import org.springframework.http.HttpStatus;

import com.tien.aivirabackend.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AnalyticsErrorCode implements ErrorCode {
    INVALID_ANONYMOUS_ID("E10600", "Anonymous identifier is invalid.", HttpStatus.BAD_REQUEST),
    INVALID_SESSION_ID("E10601", "Session identifier is invalid.", HttpStatus.BAD_REQUEST),
    VIEWER_ID_REQUIRED("E10602", "Anonymous identifier is required for guest views.", HttpStatus.BAD_REQUEST),
    INVALID_VIEW_SOURCE("E10603", "Product view source is invalid.", HttpStatus.BAD_REQUEST),
    INVALID_REFERRER_PATH("E10604", "Referrer path must be an internal path.", HttpStatus.BAD_REQUEST),
    PRODUCT_NOT_VIEWABLE("E10605", "Product is not available for viewing.", HttpStatus.NOT_FOUND),
    RECENTLY_VIEWED_NOT_FOUND("E10606", "Recently viewed product was not found.", HttpStatus.NOT_FOUND),
    VIEW_RATE_LIMIT_EXCEEDED("E10607", "Too many product view requests.", HttpStatus.TOO_MANY_REQUESTS),
    ANONYMOUS_HISTORY_CLAIM_FAILED("E10608", "Could not claim anonymous view history.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
