package com.tien.aivirabackend.domain.dto.response;

import com.tien.aivirabackend.constant.PredefinedRole;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleResponse {
    Long id;
    PredefinedRole code;
    String description;
}
