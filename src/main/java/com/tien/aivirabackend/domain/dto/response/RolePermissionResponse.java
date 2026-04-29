package com.tien.aivirabackend.domain.dto.response;

import java.util.Set;

import com.tien.aivirabackend.constant.PredefinedRole;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Role with assigned permissions")
public class RolePermissionResponse {
    Long id;

    PredefinedRole code;

    String description;

    Set<PermissionResponse> permissions;
}
