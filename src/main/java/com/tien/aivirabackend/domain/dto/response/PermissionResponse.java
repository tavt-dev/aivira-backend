package com.tien.aivirabackend.domain.dto.response;

import com.tien.aivirabackend.constant.PermissionCode;
import com.tien.aivirabackend.constant.PermissionGroup;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Permission response")
public class PermissionResponse {
    Long id;

    PermissionCode code;

    String name;

    String description;

    PermissionGroup group;

    Boolean system;
}
