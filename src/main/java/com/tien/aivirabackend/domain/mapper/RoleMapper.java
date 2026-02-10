package com.tien.aivirabackend.domain.mapper;

import com.tien.aivirabackend.domain.dto.response.RoleResponse;
import com.tien.aivirabackend.domain.entity.user.Role;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    RoleResponse toRoleResponse(Role role);
}
