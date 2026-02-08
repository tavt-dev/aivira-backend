package com.tien.aivirabackend.domain.dto.request;

import static lombok.AccessLevel.PRIVATE;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = PRIVATE)
public class UserRegisterRequest {
    @Size(min = 4, message = "Tên đăng nhập không được nhỏ hơn 4 ký tự")
    String username;

    @Size(min = 6, message = "Mật khẩu không được nhỏ hơn 6 ký tự")
    String password;

    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Email không được để trống")
    String email;

    String firstName;
    String lastName;
}
