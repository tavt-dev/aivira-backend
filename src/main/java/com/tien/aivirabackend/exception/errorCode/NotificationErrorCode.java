package com.tien.aivirabackend.exception.errorCode;

import org.springframework.http.HttpStatus;

import com.tien.aivirabackend.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {
    NOTIFICATION_NOT_FOUND("NOTIFICATION-001", "Notification not found", HttpStatus.NOT_FOUND),
    NOTIFICATION_INVALID_FILTER("NOTIFICATION-002", "Invalid notification filter", HttpStatus.BAD_REQUEST),
    NOTIFICATION_STREAM_LIMIT_EXCEEDED("NOTIFICATION-003", "Notification stream connection limit exceeded",
            HttpStatus.TOO_MANY_REQUESTS);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
