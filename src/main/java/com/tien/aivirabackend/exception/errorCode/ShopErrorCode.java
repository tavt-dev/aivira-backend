package com.tien.aivirabackend.exception.errorCode;

import org.springframework.http.HttpStatus;

import com.tien.aivirabackend.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ShopErrorCode implements ErrorCode {
    SHOP_NOT_FOUND("E9000", "Shop not found.", HttpStatus.NOT_FOUND),
    SHOP_ALREADY_EXISTS("E9001", "Current user already has a shop.", HttpStatus.CONFLICT),
    SHOP_SLUG_ALREADY_EXISTS("E9002", "Shop slug already exists.", HttpStatus.CONFLICT),
    SHOP_NOT_OWNER("E9003", "Current user is not the shop owner.", HttpStatus.FORBIDDEN),
    SHOP_NOT_APPROVED("E9004", "Shop is not approved.", HttpStatus.FORBIDDEN),
    SHOP_INVALID_STATUS_TRANSITION("E9005", "Invalid shop status transition.", HttpStatus.BAD_REQUEST),
    SHOP_UPDATE_NOT_ALLOWED("E9006", "Shop cannot be updated in current status.", HttpStatus.BAD_REQUEST),
    SHOP_LOGO_UPLOAD_FAILED("E9007", "Shop logo upload failed.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
