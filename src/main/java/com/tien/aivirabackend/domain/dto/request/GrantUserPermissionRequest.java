package com.tien.aivirabackend.domain.dto.request;

import java.time.Instant;

import com.tien.aivirabackend.constant.PermissionCode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GrantUserPermissionRequest {
    @NotNull(message = "Permission code is required")
    PermissionCode permissionCode;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    String reason;

    Instant expiresAt;
}
