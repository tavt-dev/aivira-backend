package com.tien.aivirabackend.domain.dto.response;

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
@Schema(description = "Google OAuth authorization redirect target")
public class GoogleOAuthAuthorizationResponse {
    @Schema(description = "Google authorization URL")
    String authorizationUrl;
}
