package com.tien.aivirabackend.exception.errorCode;

import org.springframework.http.HttpStatus;

import com.tien.aivirabackend.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AccountErrorCode implements ErrorCode {
    ACCOUNT_DISABLED("E2200", "Tài khoản của bạn đã bị vô hiệu hóa.", HttpStatus.FORBIDDEN),
    ACCOUNT_LOCKED("E2201", "Tài khoản đã bị khóa do đăng nhập sai quá nhiều lần.", HttpStatus.FORBIDDEN),
    ACCOUNT_NOT_VERIFIED("E2202", "Vui lòng xác minh email để tiếp tục.", HttpStatus.FORBIDDEN),
    ACCOUNT_SUSPENDED("E2203", "Tài khoản của bạn đã bị tạm khóa.", HttpStatus.FORBIDDEN),
    ACCOUNT_DELETED("E2204", "Tài khoản này đã bị xóa.", HttpStatus.GONE);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
