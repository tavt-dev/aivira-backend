package com.tien.aivirabackend.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Refresh-token request")
public class RefreshTokenRequest {
    @Schema(description = "Refresh token returned by login or refresh-token API")
    String refreshToken;
}
