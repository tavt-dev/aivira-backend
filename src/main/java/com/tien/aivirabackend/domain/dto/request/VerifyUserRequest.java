package com.tien.aivirabackend.domain.dto.request;

import static lombok.AccessLevel.PRIVATE;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = PRIVATE)
@Schema(description = "User verification request")
public class VerifyUserRequest {
    @Schema(description = "Registered email address", example = "postman_user@example.com")
    @NotBlank
    String email;

    @Schema(description = "Registration OTP code", example = "123456")
    @NotBlank
    String otpCode;
}
