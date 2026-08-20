package com.tien.aivirabackend.exception.errorCode;

import org.springframework.http.HttpStatus;

import com.tien.aivirabackend.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JwtErrorCode implements ErrorCode {

    // === E21xx: JWT / Token ===
    TOKEN_EXPIRED("E2100", "Token đã hết hạn.", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID("E2101", "Token không hợp lệ.", HttpStatus.UNAUTHORIZED),
    TOKEN_REVOKED("E2102", "Token đã bị thu hồi.", HttpStatus.UNAUTHORIZED),
    TOKEN_MALFORMED("E2103", "Token không đúng định dạng.", HttpStatus.UNAUTHORIZED),

    REFRESH_TOKEN_EXPIRED("E2104", "Refresh token đã hết hạn. Vui lòng đăng nhập lại.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_INVALID("E2105", "Refresh token không hợp lệ.", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_REUSED("E2106", "Refresh token đã được sử dụng lại. Vui lòng đăng nhập lại.",
            HttpStatus.UNAUTHORIZED),

    TOKEN_MISSING("E2107", "Thiếu token xác thực.", HttpStatus.UNAUTHORIZED),
    TOKEN_UNSUPPORTED("E2108", "Token không được hỗ trợ.", HttpStatus.UNAUTHORIZED),
    TOKEN_TYPE_INVALID("E2109", "Loại token không hợp lệ.", HttpStatus.UNAUTHORIZED),
    TOKEN_HASHING_FAILED("E2110", "Lỗi trong quá trình băm token.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
