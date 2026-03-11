package com.tien.aivirabackend.exception.errorCode;

import org.springframework.http.HttpStatus;

import com.tien.aivirabackend.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OtpErrorCode implements ErrorCode {

    OTP_NOT_FOUND("E5000", "Không tìm thấy mã OTP hợp lệ. Vui lòng yêu cầu mã mới.", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED("E5001", "Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới.", HttpStatus.BAD_REQUEST),
    OTP_INVALID("E5002", "Mã OTP không chính xác. Vui lòng thử lại.", HttpStatus.BAD_REQUEST),
    OTP_ALREADY_USED("E5003", "Mã OTP đã được sử dụng.", HttpStatus.BAD_REQUEST),
    OTP_REQUEST_TOO_FREQUENT("E5004", "Bạn đã yêu cầu mã OTP quá nhiều lần. Vui lòng đợi trước khi thử lại.", HttpStatus.TOO_MANY_REQUESTS);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
