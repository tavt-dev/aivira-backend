package com.tien.aivirabackend.exception.errorCode;

import org.springframework.http.HttpStatus;

import com.tien.aivirabackend.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CategoryErrorCode implements ErrorCode {
    CATEGORY_NOT_FOUND("E10000", "Category not found.", HttpStatus.NOT_FOUND),
    CATEGORY_NAME_ALREADY_EXISTS("E10001", "Category name already exists.", HttpStatus.CONFLICT),
    CATEGORY_SLUG_ALREADY_EXISTS("E10002", "Category slug already exists.", HttpStatus.CONFLICT),
    CATEGORY_PARENT_INVALID("E10003", "Category parent is invalid.", HttpStatus.BAD_REQUEST),
    CATEGORY_PARENT_CYCLE("E10004", "Category parent creates a cycle.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
