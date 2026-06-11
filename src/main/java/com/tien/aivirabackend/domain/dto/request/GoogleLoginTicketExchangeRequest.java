package com.tien.aivirabackend.domain.dto.request;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Request to exchange a one-time Google login ticket for Aivira tokens")
public class GoogleLoginTicketExchangeRequest {
    @NotBlank
    @Schema(description = "One-time ticket returned by Google OAuth callback redirect")
    String ticket;
}
