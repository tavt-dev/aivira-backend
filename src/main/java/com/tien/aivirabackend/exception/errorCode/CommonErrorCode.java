package com.tien.aivirabackend.exception.errorCode;

import org.springframework.http.HttpStatus;

import com.tien.aivirabackend.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {
    // === E10xx: Lỗi hệ thống / máy chủ ===
    INTERNAL_ERROR("E1000", "Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau.", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE(
            "E1001", "Dịch vụ hiện tạm thời không khả dụng. Vui lòng thử lại sau.", HttpStatus.SERVICE_UNAVAILABLE),
    TIMEOUT("E1002", "Yêu cầu đã hết thời gian chờ. Vui lòng thử lại.", HttpStatus.REQUEST_TIMEOUT),
    DATABASE_ERROR("E1003", "Xảy ra lỗi khi thao tác với cơ sở dữ liệu.", HttpStatus.INTERNAL_SERVER_ERROR),
    EXTERNAL_SERVICE_ERROR("E1004", "Dịch vụ bên ngoài không phản hồi.", HttpStatus.BAD_GATEWAY),

    // === E11xx: Lỗi xác thực dữ liệu (Validation Errors) ===
    VALIDATION_FAILED(
            "E1100", "Xác thực dữ liệu thất bại. Vui lòng kiểm tra lại thông tin nhập.", HttpStatus.BAD_REQUEST),
    INVALID_INPUT("E1101", "Dữ liệu nhập vào không hợp lệ.", HttpStatus.BAD_REQUEST),
    MISSING_REQUIRED_FIELD("E1102", "Thiếu trường dữ liệu bắt buộc.", HttpStatus.BAD_REQUEST),
    INVALID_FORMAT("E1103", "Định dạng dữ liệu không hợp lệ.", HttpStatus.BAD_REQUEST),
    VALUE_OUT_OF_RANGE("E1104", "Giá trị nằm ngoài phạm vi cho phép.", HttpStatus.BAD_REQUEST),
    INVALID_JSON("E1105", "Định dạng JSON không hợp lệ.", HttpStatus.BAD_REQUEST),

    // === E12xx: Lỗi tài nguyên (Resource Errors) ===
    RESOURCE_NOT_FOUND("E1200", "Không tìm thấy tài nguyên được yêu cầu.", HttpStatus.NOT_FOUND),
    RESOURCE_ALREADY_EXISTS("E1201", "Tài nguyên đã tồn tại.", HttpStatus.CONFLICT),
    RESOURCE_CONFLICT("E1202", "Xảy ra xung đột tài nguyên.", HttpStatus.CONFLICT),
    RESOURCE_LOCKED("E1203", "Tài nguyên hiện đang bị khóa.", HttpStatus.LOCKED),
    RESOURCE_DELETED("E1204", "Tài nguyên đã bị xóa.", HttpStatus.GONE),

    // === E13xx: Lỗi truy cập / bảo mật ===
    UNAUTHORIZED("E1300", "Yêu cầu xác thực để tiếp tục.", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("E1301", "Bạn không có quyền truy cập tài nguyên này.", HttpStatus.FORBIDDEN),
    RATE_LIMIT_EXCEEDED("E1302", "Quá nhiều yêu cầu. Vui lòng giảm tần suất truy cập.", HttpStatus.TOO_MANY_REQUESTS),
    METHOD_NOT_ALLOWED("E1303", "Phương thức HTTP không được hỗ trợ.", HttpStatus.METHOD_NOT_ALLOWED),
    INVALID_API_KEY("E1304", "API key không hợp lệ.", HttpStatus.UNAUTHORIZED);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
