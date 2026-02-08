package com.tien.aivirabackend.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    String getCode();

    String getMessage();

    HttpStatus getHttpStatus();

    default String formatMessage(Map<String, Object> attributes) {
        String msg = getMessage();

        if (attributes == null || attributes.isEmpty()) {
            return msg;
        }

        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            msg = msg.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return msg;
    }
}
