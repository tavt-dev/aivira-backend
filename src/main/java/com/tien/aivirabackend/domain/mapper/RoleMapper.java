package com.tien.aivirabackend.domain.mapper;

import org.mapstruct.Mapper;

import com.tien.aivirabackend.domain.dto.response.RoleResponse;
import com.tien.aivirabackend.domain.entity.user.Role;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    RoleResponse toRoleResponse(Role role);
}
