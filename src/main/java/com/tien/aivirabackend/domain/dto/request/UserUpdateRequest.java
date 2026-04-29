package com.tien.aivirabackend.domain.dto.request;

import static lombok.AccessLevel.PRIVATE;

import jakarta.validation.constraints.Size;

import com.tien.aivirabackend.constant.Gender;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = PRIVATE)
@Schema(description = "Current-user profile update request")
public class UserUpdateRequest {
    @Schema(description = "First name", example = "Updated", maxLength = 50)
    @Size(max = 50, message = "First name must not exceed 50 characters")
    String firstName;

    @Schema(description = "Last name", example = "User", maxLength = 50)
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    String lastName;

    @Schema(
            description = "Gender",
            example = "MALE",
            allowableValues = {"MALE", "FEMALE", "OTHER"})
    Gender gender;
}
