package com.tien.aivirabackend.service.rbac;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tien.aivirabackend.constant.PermissionCode;
import com.tien.aivirabackend.constant.PredefinedRole;
import com.tien.aivirabackend.domain.dto.request.UpdateRolePermissionsRequest;
import com.tien.aivirabackend.domain.dto.response.PermissionResponse;
import com.tien.aivirabackend.domain.dto.response.RolePermissionResponse;
import com.tien.aivirabackend.domain.entity.user.Permission;
import com.tien.aivirabackend.domain.entity.user.Role;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.CommonErrorCode;
import com.tien.aivirabackend.exception.errorCode.UserErrorCode;
import com.tien.aivirabackend.repository.PermissionRepository;
import com.tien.aivirabackend.repository.RoleRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j(topic = "PERMISSION-SERVICE")
public class PermissionServiceImpl implements PermissionService {
    PermissionRepository permissionRepository;
    RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .sorted(Comparator.comparing(Permission::getGroup).thenComparing(Permission::getCode))
                .map(this::toPermissionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolePermissionResponse> getAllRolesWithPermissions() {
        return roleRepository.findAllByOrderByCodeAsc().stream()
                .map(this::toRolePermissionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RolePermissionResponse getRolePermissions(PredefinedRole roleCode) {
        Role role = roleRepository
                .findWithPermissionsByCode(roleCode)
                .orElseThrow(() -> new AppException(UserErrorCode.ROLE_NOT_FOUND));
        return toRolePermissionResponse(role);
    }

    @Override
    @Transactional
    public RolePermissionResponse updateRolePermissions(PredefinedRole roleCode, UpdateRolePermissionsRequest request) {
        Role role = roleRepository
                .findWithPermissionsByCode(roleCode)
                .orElseThrow(() -> new AppException(UserErrorCode.ROLE_NOT_FOUND));

        Set<PermissionCode> requestedCodes =
                request.getPermissions() == null || request.getPermissions().isEmpty()
                        ? EnumSet.noneOf(PermissionCode.class)
                        : EnumSet.copyOf(request.getPermissions());

        validateRequiredRolePermissions(roleCode, requestedCodes);

        List<Permission> permissions = permissionRepository.findByCodeIn(requestedCodes);
        if (permissions.size() != requestedCodes.size()) {
            throw new AppException(CommonErrorCode.RESOURCE_NOT_FOUND);
        }

        role.setPermissions(new HashSet<>(permissions));
        Role savedRole = roleRepository.save(role);
        log.info("Updated permissions for role {}: {}", roleCode, requestedCodes);
        return toRolePermissionResponse(savedRole);
    }

    @Override
    @Transactional
    public void seedDefaultPermissions() {
        Map<PermissionCode, Permission> permissionsByCode = seedPermissionCatalog();

        for (PredefinedRole roleCode : PredefinedRole.values()) {
            Role role = roleRepository
                    .findWithPermissionsByCode(roleCode)
                    .orElseGet(() -> roleRepository.save(Role.builder()
                            .code(roleCode)
                            .description(roleCode.name() + " ROLE")
                            .permissions(new HashSet<>())
                            .build()));

            if (role.getPermissions() == null) {
                role.setPermissions(new HashSet<>());
            }

            Set<PermissionCode> defaultCodes = DefaultPermissionMatrix.forRole(roleCode);
            Set<PermissionCode> currentCodes =
                    role.getPermissions().stream().map(Permission::getCode).collect(Collectors.toSet());

            Set<Permission> missingPermissions = defaultCodes.stream()
                    .map(permissionsByCode::get)
                    .filter(Objects::nonNull)
                    .filter(permission -> !currentCodes.contains(permission.getCode()))
                    .collect(Collectors.toSet());

            if (!missingPermissions.isEmpty()) {
                role.getPermissions().addAll(missingPermissions);
                roleRepository.save(role);
                log.info("Seeded {} missing permissions for role {}", missingPermissions.size(), roleCode);
            }
        }
    }

    private Map<PermissionCode, Permission> seedPermissionCatalog() {
        List<Permission> existingPermissions = permissionRepository.findAll();
        Map<PermissionCode, Permission> permissionsByCode =
                existingPermissions.stream().collect(Collectors.toMap(Permission::getCode, Function.identity()));

        for (PermissionCode code : PermissionCode.values()) {
            permissionsByCode.computeIfAbsent(
                    code,
                    permissionCode -> permissionRepository.save(Permission.builder()
                            .code(permissionCode)
                            .name(toDisplayName(permissionCode))
                            .description("System permission " + permissionCode.name())
                            .group(permissionCode.getGroup())
                            .system(true)
                            .build()));
        }

        return permissionsByCode;
    }

    private void validateRequiredRolePermissions(PredefinedRole roleCode, Set<PermissionCode> requestedCodes) {
        if (roleCode == PredefinedRole.ADMIN
                && (!requestedCodes.contains(PermissionCode.ROLE_MANAGE)
                        || !requestedCodes.contains(PermissionCode.PERMISSION_MANAGE))) {
            throw new AppException(UserErrorCode.CANNOT_REMOVE_ROLE);
        }
    }

    private RolePermissionResponse toRolePermissionResponse(Role role) {
        Set<PermissionResponse> permissions = role.getPermissions() == null
                ? Set.of()
                : role.getPermissions().stream()
                        .sorted(Comparator.comparing(Permission::getGroup).thenComparing(Permission::getCode))
                        .map(this::toPermissionResponse)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        return RolePermissionResponse.builder()
                .id(role.getId())
                .code(role.getCode())
                .description(role.getDescription())
                .permissions(permissions)
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

    private String toDisplayName(PermissionCode code) {
        String lower = code.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
