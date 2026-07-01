package com.tien.aivirabackend.exception;

import java.util.HashMap;
import java.util.Map;
import javax.security.sasl.AuthenticationException;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.exception.errorCode.AccountErrorCode;
import com.tien.aivirabackend.exception.errorCode.AuthErrorCode;
import com.tien.aivirabackend.exception.errorCode.CommonErrorCode;
import com.tien.aivirabackend.exception.errorCode.FileValidationErrorCode;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Core - Custom Application Exception Handler
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleAppException(AppException ex) {

        log.warn("AppException [{}] {}", ex.getErrorCode().getCode(), ex.getMessage());

        return ResponseEntity.status(ex.getErrorCode().getHttpStatus())
                .body(ApiResponse.error(
                        ex.getErrorCode().getCode(), ex.getMessage(), ex.hasDetails() ? ex.getDetails() : null));
    }

    // SECURITY

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials() {
        return build(AuthErrorCode.INVALID_CREDENTIALS);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication() {
        return build(AuthErrorCode.AUTHENTICATION_FAILED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied() {
        return build(CommonErrorCode.ACCESS_DENIED);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLocked() {
        return build(AccountErrorCode.ACCOUNT_LOCKED);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabled() {
        return build(AccountErrorCode.ACCOUNT_DISABLED);
    }

    /* ================= VALIDATION ================= */

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        return ResponseEntity.status(CommonErrorCode.VALIDATION_FAILED.getHttpStatus())
                .body(ApiResponse.error(
                        CommonErrorCode.VALIDATION_FAILED.getCode(),
                        CommonErrorCode.VALIDATION_FAILED.getMessage(),
                        errors));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest() {
        return build(CommonErrorCode.INVALID_INPUT);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded() {
        return build(FileValidationErrorCode.FILE_TOO_LARGE);
    }

    /* ================= FALLBACK ================= */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnhandled(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(CommonErrorCode.INTERNAL_ERROR);
    }

    /* ================= UTIL ================= */

    private ResponseEntity<ApiResponse<Void>> build(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }
}
