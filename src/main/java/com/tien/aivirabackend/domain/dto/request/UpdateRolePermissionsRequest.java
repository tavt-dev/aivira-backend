package com.tien.aivirabackend.domain.dto.request;

import java.util.Set;

import com.tien.aivirabackend.constant.PermissionCode;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateRolePermissionsRequest {
    @NotNull(message = "Permission codes are required")
    Set<PermissionCode> permissions;
}
