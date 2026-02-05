package com.tien.aivirabackend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

@Getter
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> attributes;
    private final Map<String, Object> details;

    public AppException(ErrorCode errorCode) {
        this(errorCode, Map.of(), Map.of(), null);
    }

    public AppException(ErrorCode errorCode, Map<String, Object> attributes) {
        this(errorCode, attributes, Map.of(), null);
    }

    public AppException(ErrorCode errorCode, Throwable cause) {
        this(errorCode, Map.of(), Map.of(), cause);
    }

    public AppException(
            ErrorCode errorCode,
            Map<String, Object> attributes,
            Map<String, Object> details,
            Throwable cause
    ) {
        super(errorCode.formatMessage(attributes), cause);
        this.errorCode = errorCode;
        this.attributes = new HashMap<>(attributes);
        this.details = new HashMap<>(details);
    }

    /* ===== fluent ===== */

    public AppException addAttribute(String key, Object value) {
        attributes.put(key, value);
        return this;
    }

    public AppException addDetail(String key, Object value) {
        details.put(key, value);
        return this;
    }

    /* ===== helpers ===== */

    public boolean hasDetails() {
        return !details.isEmpty();
    }
}
