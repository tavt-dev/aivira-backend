package com.tien.aivirabackend.domain.dto.request;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Forgot-password request")
public class ForgotPasswordRequest {
    @Schema(description = "Verified email address", example = "postman_user@example.com")
    @NotBlank(message = "Email không được để trống")
    String email;
}
