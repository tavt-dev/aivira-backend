package com.tien.aivirabackend.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Reset-password request")
public class ResetPasswordRequest {
    @Schema(description = "Verified email address", example = "postman_user@example.com")
    @NotBlank
    String email;

    @Schema(description = "Password-reset OTP code", example = "123456")
    @NotBlank
    String otpCode;

    @Schema(description = "New password", example = "NewPassword123!")
    @NotBlank
    String newPassword;
}
