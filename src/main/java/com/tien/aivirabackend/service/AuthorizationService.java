package com.tien.aivirabackend.service;

public interface AuthorizationService {
    boolean hasPermission(String permissionCode);

    boolean hasAnyPermission(String... permissionCodes);
}
