package com.tien.aivirabackend.exception.errorCode;

import org.springframework.http.HttpStatus;

import com.tien.aivirabackend.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    // === E30xx: Lỗi tra cứu người dùng (User Lookup Errors) ===
    USER_NOT_FOUND("E3000", "Không tìm thấy người dùng.", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND_BY_EMAIL("E3001", "Không tìm thấy người dùng với email này.", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND_BY_PHONE("E3002", "Không tìm thấy người dùng với số điện thoại này.", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND_BY_ID("E3003", "Không tìm thấy người dùng với ID này.", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND_BY_USERNAME("E3004", "Không tìm thấy người dùng với tên người dùng này.", HttpStatus.NOT_FOUND),

    // === E31xx: Lỗi dữ liệu người dùng (User Data Errors) ===
    EMAIL_ALREADY_EXISTS("E3100", "Email này đã được sử dụng.", HttpStatus.CONFLICT),
    PHONE_ALREADY_EXISTS("E3101", "Số điện thoại này đã được sử dụng.", HttpStatus.CONFLICT),
    USERNAME_ALREADY_EXISTS("E3102", "Tên người dùng này đã tồn tại.", HttpStatus.CONFLICT),
    INVALID_EMAIL_FORMAT("E3103", "Định dạng email không hợp lệ.", HttpStatus.BAD_REQUEST),
    INVALID_PHONE_FORMAT("E3104", "Định dạng số điện thoại không hợp lệ.", HttpStatus.BAD_REQUEST),
    USER_ALREADY_VERIFIED("E3105", "Tài khoản đã được xác thực.", HttpStatus.BAD_REQUEST),
    EMAIL_NOT_VERIFIED("E3106", "Email chưa được xác thực.", HttpStatus.BAD_REQUEST),

    // === E32xx: Lỗi hồ sơ / tài khoản người dùng (User Profile & Account Errors) ===
    PROFILE_UPDATE_FAILED("E3200", "Cập nhật hồ sơ thất bại.", HttpStatus.INTERNAL_SERVER_ERROR),
    AVATAR_UPLOAD_FAILED("E3201", "Tải ảnh đại diện thất bại.", HttpStatus.INTERNAL_SERVER_ERROR),
    AVATAR_TOO_LARGE("E3202", "Kích thước ảnh đại diện vượt quá giới hạn cho phép.", HttpStatus.BAD_REQUEST),
    INVALID_AVATAR_FORMAT("E3203", "Định dạng ảnh đại diện không hợp lệ. Hỗ trợ: JPG, PNG, WEBP.", HttpStatus.BAD_REQUEST),
    PROFILE_INCOMPLETE("E3204", "Vui lòng hoàn thiện hồ sơ của bạn.", HttpStatus.BAD_REQUEST),
    USER_PASSWORD_NOT_SET("E3205", "Người dùng chưa thiết lập mật khẩu.", HttpStatus.BAD_REQUEST),
    INVALID_CURRENT_PASSWORD("E3206", "Mật khẩu hiện tại không chính xác.", HttpStatus.BAD_REQUEST),
    PASSWORD_CONFIRMATION_DOES_NOT_MATCH("E3207", "Xác nhận mật khẩu không khớp.", HttpStatus.BAD_REQUEST),
    NEW_PASSWORD_MUST_BE_DIFFERENT("E3208", "Mật khẩu mới phải khác mật khẩu hiện tại.", HttpStatus.BAD_REQUEST),
    USER_ACCOUNT_INACTIVE("E3209", "Tài khoản người dùng chưa được kích hoạt.", HttpStatus.FORBIDDEN),
    USER_ACCOUNT_LOCKED("E3210", "Tài khoản người dùng đã bị khóa.", HttpStatus.FORBIDDEN),
    USER_ACCOUNT_DELETED("E3211", "Tài khoản người dùng không còn khả dụng.", HttpStatus.FORBIDDEN),

    // === E33xx: Lỗi vai trò / phân quyền người dùng (User Role/Permission Errors) ===
    ROLE_NOT_FOUND("E3300", "Không tìm thấy vai trò.", HttpStatus.NOT_FOUND),
    CANNOT_ASSIGN_ROLE("E3301", "Không thể gán vai trò này.", HttpStatus.FORBIDDEN),
    CANNOT_REMOVE_ROLE("E3302", "Không thể gỡ bỏ vai trò này.", HttpStatus.FORBIDDEN),
    CANNOT_DELETE_SELF("E3303", "Bạn không thể tự xóa tài khoản của mình.", HttpStatus.BAD_REQUEST),
    CANNOT_MODIFY_ADMIN("E3304", "Không thể chỉnh sửa tài khoản quản trị.", HttpStatus.FORBIDDEN),
    INSUFFICIENT_PERMISSIONS("E3305", "Bạn không đủ quyền để thực hiện thao tác này.", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}