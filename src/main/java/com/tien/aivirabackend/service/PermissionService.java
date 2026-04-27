package com.tien.aivirabackend.service;

import java.util.List;

import com.tien.aivirabackend.constant.PredefinedRole;
import com.tien.aivirabackend.domain.dto.request.UpdateRolePermissionsRequest;
import com.tien.aivirabackend.domain.dto.response.PermissionResponse;
import com.tien.aivirabackend.domain.dto.response.RolePermissionResponse;

public interface PermissionService {
    List<PermissionResponse> getAllPermissions();

    List<RolePermissionResponse> getAllRolesWithPermissions();

    RolePermissionResponse getRolePermissions(PredefinedRole roleCode);

    RolePermissionResponse updateRolePermissions(PredefinedRole roleCode, UpdateRolePermissionsRequest request);

    void seedDefaultPermissions();
}
