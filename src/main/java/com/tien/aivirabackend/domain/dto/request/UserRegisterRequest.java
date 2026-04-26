package com.tien.aivirabackend.domain.dto.request;

import static lombok.AccessLevel.PRIVATE;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = PRIVATE)
@Schema(description = "User registration request")
public class UserRegisterRequest {
    @Schema(description = "Unique username", example = "postman_user", minLength = 4)
    @Size(min = 4, message = "Tên đăng nhập không được nhỏ hơn 4 ký tự")
    String username;

    @Schema(description = "Local account password", example = "Password123!", minLength = 6)
    @Size(min = 6, message = "Mật khẩu không được nhỏ hơn 6 ký tự")
    String password;

    @Schema(description = "User email address", example = "postman_user@example.com")
    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Email không được để trống")
    String email;

    @Schema(description = "First name", example = "Test")
    String firstName;

    @Schema(description = "Last name", example = "User")
    String lastName;
}
