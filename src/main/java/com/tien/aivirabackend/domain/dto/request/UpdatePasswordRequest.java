package com.tien.aivirabackend.domain.dto.request;

import static lombok.AccessLevel.PRIVATE;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = PRIVATE)
public class UpdatePasswordRequest {
    @NotBlank(message = "Mật khẩu cũ không được để trống")
    String currentPassword;

    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    String confirmPassword;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    String newPassword;
}
