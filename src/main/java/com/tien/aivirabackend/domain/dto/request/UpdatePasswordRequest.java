package com.tien.aivirabackend.domain.dto.request;

import static lombok.AccessLevel.PRIVATE;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = PRIVATE)
@Schema(description = "Current-user password change request")
public class UpdatePasswordRequest {
    @Schema(description = "Current password", example = "Password123!")
    @NotBlank(message = "Mật khẩu cũ không được để trống")
    String currentPassword;

    @Schema(description = "Confirmation of the new password", example = "NewPassword123!")
    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    String confirmPassword;

    @Schema(description = "New password", example = "NewPassword123!")
    @NotBlank(message = "Mật khẩu mới không được để trống")
    String newPassword;
}
