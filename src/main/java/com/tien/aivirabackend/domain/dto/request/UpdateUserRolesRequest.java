package com.tien.aivirabackend.domain.dto.request;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;

import com.tien.aivirabackend.constant.PredefinedRole;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@Schema(description = "Update user roles request")
public class UpdateUserRolesRequest {
    @NotEmpty
    Set<PredefinedRole> roles;
}
