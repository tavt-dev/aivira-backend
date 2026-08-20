package com.tien.aivirabackend.exception.errorCode;

import org.springframework.http.HttpStatus;

import com.tien.aivirabackend.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PasswordErrorCode implements ErrorCode {
    PASSWORD_INCORRECT("E2300", "Mật khẩu hiện tại không đúng.", HttpStatus.BAD_REQUEST),
    PASSWORD_TOO_WEAK("E2301", "Mật khẩu không đáp ứng yêu cầu bảo mật.", HttpStatus.BAD_REQUEST),
    PASSWORD_RECENTLY_USED("E2302", "Mật khẩu này đã được sử dụng gần đây. Vui lòng chọn mật khẩu khác.",
            HttpStatus.BAD_REQUEST),
    PASSWORD_RESET_REQUIRED("E2303", "Bạn cần đặt lại mật khẩu.", HttpStatus.FORBIDDEN),
    PASSWORD_RESET_TOKEN_EXPIRED("E2304", "Liên kết đặt lại mật khẩu đã hết hạn.", HttpStatus.BAD_REQUEST),
    PASSWORD_MISMATCH("E2305", "Mật khẩu xác nhận không khớp.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
