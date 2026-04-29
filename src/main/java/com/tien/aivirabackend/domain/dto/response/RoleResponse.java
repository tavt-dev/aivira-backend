package com.tien.aivirabackend.domain.dto.response;

import com.tien.aivirabackend.constant.PredefinedRole;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Role response")
public class RoleResponse {
    @Schema(description = "Role database ID", example = "1")
    Long id;

    @Schema(description = "Predefined role code", example = "USER")
    PredefinedRole code;

    @Schema(description = "Role description")
    String description;
}
