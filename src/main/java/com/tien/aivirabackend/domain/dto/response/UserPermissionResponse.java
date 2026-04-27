package com.tien.aivirabackend.domain.dto.response;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Direct permission assigned to a user")
public class UserPermissionResponse {
    Long id;

    PermissionResponse permission;

    String reason;

    String grantedByUserId;

    Instant grantedAt;

    Instant expiresAt;

    Instant revokedAt;

    Boolean active;

    Boolean currentlyActive;
}
