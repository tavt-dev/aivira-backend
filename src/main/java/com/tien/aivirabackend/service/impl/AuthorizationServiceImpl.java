package com.tien.aivirabackend.service.impl;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.tien.aivirabackend.constant.PermissionCode;
import com.tien.aivirabackend.repository.UserPermissionRepository;
import com.tien.aivirabackend.repository.UserRepository;
import com.tien.aivirabackend.service.AuthorizationService;

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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            return null;
        }

        return jwt.getClaimAsString("user_id");
    }
}
