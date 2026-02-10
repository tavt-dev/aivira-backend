package com.tien.aivirabackend.domain.dto.response;

import java.time.Instant;
import java.util.Set;

import com.tien.aivirabackend.constant.Gender;
import com.tien.aivirabackend.constant.SignInProvider;
import com.tien.aivirabackend.domain.entity.user.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class UserResponse {
    String id;
    String username;
    String email;
    String firstName;
    String lastName;

    SignInProvider provider;
    Gender gender;

    String phoneNumber;

    String avatarUrl;

    Set<RoleResponse> roles;
    Instant createdAt;
    Instant updatedAt;
}
