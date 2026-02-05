package com.tien.aivirabackend.domain.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
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
