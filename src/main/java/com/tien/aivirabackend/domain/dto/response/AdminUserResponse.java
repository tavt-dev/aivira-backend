package com.tien.aivirabackend.domain.dto.response;

import java.time.Instant;
import java.util.Set;

import com.tien.aivirabackend.constant.Gender;
import com.tien.aivirabackend.constant.SignInProvider;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Admin user response")
public class AdminUserResponse {
    String id;
    String username;
    String email;
    String firstName;
    String lastName;
    SignInProvider provider;
    Gender gender;
    String phoneNumber;
    String avatarUrl;
    Boolean isActive;
    Boolean emailVerified;
    Boolean isLocked;
    Boolean isDeleted;
    Instant lockoutUntil;
    Integer failedLoginAttempts;
    Integer tokenVersion;
    Set<RoleResponse> roles;
    Instant createdAt;
    Instant updatedAt;
}
