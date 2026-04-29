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
@Schema(description = "User profile response")
public class UserResponse {
    @Schema(description = "User ID")
    String id;

    @Schema(description = "Username", example = "postman_user")
    String username;

    @Schema(description = "Email address", example = "postman_user@example.com")
    String email;

    @Schema(description = "First name", example = "Test")
    String firstName;

    @Schema(description = "Last name", example = "User")
    String lastName;

    @Schema(description = "Sign-in provider", example = "LOCAL")
    SignInProvider provider;

    @Schema(description = "Gender", example = "MALE")
    Gender gender;

    @Schema(description = "Whether the account is active", example = "true")
    Boolean isActive;

    @Schema(description = "Whether the email is verified", example = "true")
    Boolean emailVerified;

    @Schema(description = "Phone number")
    String phoneNumber;

    @Schema(description = "Avatar URL")
    String avatarUrl;

    @Schema(description = "Assigned roles")
    Set<RoleResponse> roles;

    @Schema(description = "Creation timestamp")
    Instant createdAt;

    @Schema(description = "Last update timestamp")
    Instant updatedAt;
}
