package com.tien.aivirabackend.exception.errorCode;

import org.springframework.http.HttpStatus;

import com.tien.aivirabackend.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    // === E20xx: Lỗi xác thực (Authentication Errors) ===
    INVALID_CREDENTIALS("E2000", "Email hoặc mật khẩu không đúng.", HttpStatus.UNAUTHORIZED),
    AUTHENTICATION_FAILED("E2001", "Xác thực thất bại.", HttpStatus.UNAUTHORIZED),
    LOGIN_REQUIRED("E2002", "Vui lòng đăng nhập để tiếp tục.", HttpStatus.UNAUTHORIZED),
    INVALID_AUTHENTICATION_METHOD("E2004", "Phương thức xác thực không hợp lệ.", HttpStatus.UNAUTHORIZED);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
