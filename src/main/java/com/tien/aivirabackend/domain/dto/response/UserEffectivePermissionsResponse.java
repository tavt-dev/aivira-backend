package com.tien.aivirabackend.domain.dto.response;

import java.util.List;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Effective permissions for one user")
public class UserEffectivePermissionsResponse {
    String userId;

    Set<PermissionResponse> rolePermissions;

    List<UserPermissionResponse> directPermissions;

    Set<PermissionResponse> effectivePermissions;
}
