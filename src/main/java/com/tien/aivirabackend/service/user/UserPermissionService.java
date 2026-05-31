package com.tien.aivirabackend.service.user;

import com.tien.aivirabackend.constant.PermissionCode;
import com.tien.aivirabackend.domain.dto.request.GrantUserPermissionRequest;
import com.tien.aivirabackend.domain.dto.response.UserEffectivePermissionsResponse;
import com.tien.aivirabackend.domain.dto.response.UserPermissionResponse;

public interface UserPermissionService {
    UserEffectivePermissionsResponse getUserPermissions(String userId);

    UserPermissionResponse grantPermission(String userId, GrantUserPermissionRequest request);

    void revokePermission(String userId, PermissionCode permissionCode);
}
