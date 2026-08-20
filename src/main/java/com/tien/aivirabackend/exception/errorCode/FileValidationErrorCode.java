package com.tien.aivirabackend.exception.errorCode;

import org.springframework.http.HttpStatus;

import com.tien.aivirabackend.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FileValidationErrorCode implements ErrorCode {
    EMPTY_FILE("EMPTY_FILE", "Tệp tải lên đang trống.", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE("FILE_TOO_LARGE", "Tệp tải lên vượt quá kích thước cho phép.", HttpStatus.PAYLOAD_TOO_LARGE),
    INVALID_MIME_TYPE("INVALID_MIME_TYPE", "Loại MIME của tệp không được phép.", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    INVALID_FILE_SIGNATURE("INVALID_FILE_SIGNATURE", "Nội dung tệp không khớp với loại đã khai báo.",
            HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    UNSUPPORTED_MEDIA_CATEGORY("UNSUPPORTED_MEDIA_CATEGORY", "Loại media yêu cầu hiện không được hỗ trợ.",
            HttpStatus.BAD_REQUEST),
    FILE_READ_ERROR("FILE_READ_ERROR", "Không thể đọc tệp đã tải lên.", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
