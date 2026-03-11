package com.tien.aivirabackend.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = PRIVATE)
public class VerifyUserRequest {
    @NotBlank
    String email;

    @NotBlank
    String otpCode;
}
