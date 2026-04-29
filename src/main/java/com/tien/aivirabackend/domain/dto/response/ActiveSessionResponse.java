package com.tien.aivirabackend.domain.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Active refresh-token session")
public class ActiveSessionResponse {
    @Schema(description = "Session ID")
    String sessionId;

    @Schema(description = "Device or user-agent information")
    String deviceInfo;

    @Schema(description = "Client IP address")
    String ipAddress;

    @Schema(description = "Session creation timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    Instant createdAt;

    @Schema(description = "Session expiration timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    Instant expiresAt;

    @Schema(description = "Whether this session belongs to the current access token", example = "true")
    boolean currentSession;
}
