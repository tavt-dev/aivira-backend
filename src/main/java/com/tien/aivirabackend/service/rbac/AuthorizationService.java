package com.tien.aivirabackend.service.rbac;

public interface AuthorizationService {
    boolean hasPermission(String permissionCode);

    boolean hasAnyPermission(String... permissionCodes);
}
