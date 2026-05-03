package com.tien.aivirabackend.service.impl;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.tien.aivirabackend.constant.PermissionCode;
import com.tien.aivirabackend.repository.UserPermissionRepository;
import com.tien.aivirabackend.repository.UserRepository;
import com.tien.aivirabackend.service.AuthorizationService;
import com.tien.aivirabackend.service.CurrentUserService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service("authorizationService")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j(topic = "AUTHORIZATION-SERVICE")
public class AuthorizationServiceImpl implements AuthorizationService {
    UserRepository userRepository;
    UserPermissionRepository userPermissionRepository;
    CurrentUserService currentUserService;

    @Override
    public boolean hasPermission(String permissionCode) {
        if (!StringUtils.hasText(permissionCode)) {
            return false;
        }

        PermissionCode parsedPermission;
        try {
            parsedPermission = PermissionCode.valueOf(permissionCode);
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown permission requested in authorization check: {}", permissionCode);
            return false;
        }

        String userId = getCurrentUserId();
        if (!StringUtils.hasText(userId)) {
            return false;
        }

        Set<PermissionCode> permissions = new HashSet<>(userRepository.findRolePermissionCodesByUserId(userId));
        permissions.addAll(userPermissionRepository.findActivePermissionCodesByUserId(userId, Instant.now()));
        return permissions.contains(parsedPermission);
    }

    @Override
    public boolean hasAnyPermission(String... permissionCodes) {
        if (permissionCodes == null || permissionCodes.length == 0) {
            return false;
        }

        return Arrays.stream(permissionCodes).anyMatch(this::hasPermission);
    }

    private String getCurrentUserId() {
        return currentUserService.findCurrentUserId().orElse(null);
    }
}
