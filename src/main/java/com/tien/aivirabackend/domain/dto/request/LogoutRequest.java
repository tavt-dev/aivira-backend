package com.tien.aivirabackend.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Logout request")
public class LogoutRequest {
    @Schema(description = "Refresh token to revoke")
    String refreshToken;
}
