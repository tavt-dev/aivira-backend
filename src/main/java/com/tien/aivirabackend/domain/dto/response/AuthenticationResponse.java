package com.tien.aivirabackend.domain.dto.response;

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
@Schema(description = "Authentication token response")
public class AuthenticationResponse {
    @Schema(description = "JWT access token")
    String token;

    @Schema(description = "JWT refresh token. It can be omitted when legacy body refresh-token output is disabled.")
    String refreshToken;

    @Schema(description = "Token type", example = "Bearer")
    String tokenType;

    @Schema(description = "Access-token lifetime in seconds", example = "3600")
    long accessTokenExpiresIn;

    @Schema(description = "Whether authentication succeeded", example = "true")
    boolean authenticated;
}
