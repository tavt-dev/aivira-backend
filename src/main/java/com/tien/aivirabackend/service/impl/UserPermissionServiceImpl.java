package com.tien.aivirabackend.service.impl;

import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.tien.aivirabackend.constant.PermissionCode;
import com.tien.aivirabackend.domain.dto.request.GrantUserPermissionRequest;
import com.tien.aivirabackend.domain.dto.response.PermissionResponse;
import com.tien.aivirabackend.domain.dto.response.UserEffectivePermissionsResponse;
import com.tien.aivirabackend.domain.dto.response.UserPermissionResponse;
import com.tien.aivirabackend.domain.entity.user.Permission;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.entity.user.UserPermission;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.CommonErrorCode;
import com.tien.aivirabackend.exception.errorCode.UserErrorCode;
import com.tien.aivirabackend.repository.PermissionRepository;
import com.tien.aivirabackend.repository.UserPermissionRepository;
import com.tien.aivirabackend.repository.UserRepository;
import com.tien.aivirabackend.service.CurrentUserService;
import com.tien.aivirabackend.service.UserPermissionService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j(topic = "USER-PERMISSION-SERVICE")
public class UserPermissionServiceImpl implements UserPermissionService {
    UserRepository userRepository;
    PermissionRepository permissionRepository;
    UserPermissionRepository userPermissionRepository;
    CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public UserEffectivePermissionsResponse getUserPermissions(String userId) {
        ensureUserExists(userId);

        Instant now = Instant.now();
        Set<PermissionCode> rolePermissionCodes = userRepository.findRolePermissionCodesByUserId(userId);
        Set<PermissionCode> directActiveCodes = userPermissionRepository.findActivePermissionCodesByUserId(userId, now);

        Set<PermissionCode> effectiveCodes = EnumSet.noneOf(PermissionCode.class);
        effectiveCodes.addAll(rolePermissionCodes);
        effectiveCodes.addAll(directActiveCodes);

        Map<PermissionCode, Permission> permissionsByCode = effectiveCodes.isEmpty()
                ? Map.of()
                : permissionRepository.findByCodeIn(effectiveCodes).stream()
                        .collect(Collectors.toMap(Permission::getCode, permission -> permission));

        List<UserPermissionResponse> directPermissions = userPermissionRepository.findAllByUserId(userId).stream()
                .map(permission -> toUserPermissionResponse(permission, now))
                .toList();

        return UserEffectivePermissionsResponse.builder()
                .userId(userId)
                .rolePermissions(toPermissionResponses(rolePermissionCodes, permissionsByCode))
                .directPermissions(directPermissions)
                .effectivePermissions(toPermissionResponses(effectiveCodes, permissionsByCode))
                .build();
    }

    @Override
    @Transactional
    public UserPermissionResponse grantPermission(String userId, GrantUserPermissionRequest request) {
        String currentUserId = getCurrentUserId();
        if (StringUtils.hasText(currentUserId) && currentUserId.equals(userId)) {
            throw new AppException(CommonErrorCode.ACCESS_DENIED);
        }

        User targetUser =
                userRepository.findById(userId).orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND_BY_ID));
        User grantedBy = StringUtils.hasText(currentUserId)
                ? userRepository.findById(currentUserId).orElse(null)
                : null;
        Permission permission = permissionRepository
                .findByCode(request.getPermissionCode())
                .orElseThrow(() -> new AppException(CommonErrorCode.RESOURCE_NOT_FOUND));

        if (userPermissionRepository.existsByUser_IdAndPermission_CodeAndActiveTrue(
                userId, request.getPermissionCode())) {
            throw new AppException(CommonErrorCode.RESOURCE_ALREADY_EXISTS);
        }

        Instant now = Instant.now();
        UserPermission userPermission = UserPermission.builder()
                .user(targetUser)
                .permission(permission)
                .reason(request.getReason())
                .expiresAt(request.getExpiresAt())
                .grantedBy(grantedBy)
                .grantedAt(now)
                .active(true)
                .build();

        UserPermission saved = userPermissionRepository.save(userPermission);
        log.info(
                "Granted direct permission {} to user {} by {} expiresAt={}",
                permission.getCode(),
                userId,
                currentUserId,
                request.getExpiresAt());
        return toUserPermissionResponse(saved, now);
    }

    @Override
    @Transactional
    public void revokePermission(String userId, PermissionCode permissionCode) {
        String currentUserId = getCurrentUserId();
        if (StringUtils.hasText(currentUserId) && currentUserId.equals(userId)) {
            throw new AppException(CommonErrorCode.ACCESS_DENIED);
        }

        UserPermission userPermission = userPermissionRepository
                .findFirstByUser_IdAndPermission_CodeAndActiveTrueOrderByGrantedAtDesc(userId, permissionCode)
                .orElseThrow(() -> new AppException(CommonErrorCode.RESOURCE_NOT_FOUND));

        Instant now = Instant.now();
        userPermission.revoke(now);
        userPermissionRepository.save(userPermission);
        log.info("Revoked direct permission {} from user {} by {}", permissionCode, userId, currentUserId);
    }

    private void ensureUserExists(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new AppException(UserErrorCode.USER_NOT_FOUND_BY_ID);
        }
    }

    private Set<PermissionResponse> toPermissionResponses(
            Set<PermissionCode> codes, Map<PermissionCode, Permission> permissionsByCode) {
        if (codes == null || codes.isEmpty()) {
            return Set.of();
        }

        return codes.stream()
                .sorted()
                .map(permissionsByCode::get)
                .filter(Objects::nonNull)
                .map(this::toPermissionResponse)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private UserPermissionResponse toUserPermissionResponse(UserPermission userPermission, Instant now) {
        User grantedBy = userPermission.getGrantedBy();
        return UserPermissionResponse.builder()
                .id(userPermission.getId())
                .permission(toPermissionResponse(userPermission.getPermission()))
                .reason(userPermission.getReason())
                .grantedByUserId(grantedBy == null ? null : grantedBy.getId())
                .grantedAt(userPermission.getGrantedAt())
                .expiresAt(userPermission.getExpiresAt())
                .revokedAt(userPermission.getRevokedAt())
                .active(userPermission.getActive())
                .currentlyActive(userPermission.isCurrentlyActive(now))
                .build();
    }

    private PermissionResponse toPermissionResponse(Permission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .code(permission.getCode())
                .name(permission.getName())
                .description(permission.getDescription())
                .group(permission.getGroup())
                .system(permission.getSystem())
                .build();
    }

    private String getCurrentUserId() {
        return currentUserService.findCurrentUserId().orElse(null);
    }
}
