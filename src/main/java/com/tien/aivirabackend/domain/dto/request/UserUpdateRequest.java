package com.tien.aivirabackend.domain.dto.request;

import static lombok.AccessLevel.PRIVATE;

import jakarta.validation.constraints.Size;

import com.tien.aivirabackend.constant.Gender;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = PRIVATE)
public class UserUpdateRequest {
    @Size(max = 50, message = "First name must not exceed 50 characters")
    String firstName;

    @Size(max = 50, message = "Last name must not exceed 50 characters")
    String lastName;

    Gender gender;

    @Size(max = 15, message = "Phone number must not exceed 15 characters")
    String phoneNumber;

    String avatarUrl;
}
