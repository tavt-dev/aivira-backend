package com.tien.aivirabackend.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response envelope")
public class ApiResponse<T> {

    @Schema(description = "Whether the request completed successfully", example = "true")
    boolean success;

    @Schema(description = "Application error code. Null for successful responses.", example = "E2107")
    String errorCode;

    @Schema(description = "Human-readable response message", example = "Authentication successful")
    String message;

    @Schema(description = "Response payload")
    T data;

    @Schema(description = "Response timestamp in epoch milliseconds", example = "1766558400000")
    long timestamp;

    /* ================= SUCCESS ================= */

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T> builder().success(true).message("Success").data(data)
                .timestamp(System.currentTimeMillis()).build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T> builder().success(true).message(message).data(data).timestamp(System.currentTimeMillis())
                .build();
    }

    /* ================= ERROR ================= */

    public static ApiResponse<Void> error(String errorCode, String message) {
        return ApiResponse.<Void> builder().success(false).errorCode(errorCode).message(message)
                .timestamp(System.currentTimeMillis()).build();
    }

    public static <T> ApiResponse<T> error(String errorCode, String message, T data) {
        return ApiResponse.<T> builder().success(false).errorCode(errorCode).message(message).data(data)
                .timestamp(System.currentTimeMillis()).build();
    }
}
