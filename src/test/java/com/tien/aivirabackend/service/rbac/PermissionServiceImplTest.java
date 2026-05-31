package com.tien.aivirabackend.service.rbac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tien.aivirabackend.constant.PermissionCode;
import com.tien.aivirabackend.constant.PredefinedRole;
import com.tien.aivirabackend.domain.dto.request.UpdateRolePermissionsRequest;
import com.tien.aivirabackend.domain.entity.user.Permission;
import com.tien.aivirabackend.domain.entity.user.Role;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.repository.PermissionRepository;
import com.tien.aivirabackend.repository.RoleRepository;

@ExtendWith(MockitoExtension.class)
class PermissionServiceImplTest {
    @Mock
    PermissionRepository permissionRepository;

    @Mock
    RoleRepository roleRepository;

    @InjectMocks
    PermissionServiceImpl permissionService;

    @Test
    void updateRolePermissions_shouldAssignRequestedPermissions() {
        Role role = Role.builder()
                .id(1L)
                .code(PredefinedRole.USER)
                .description("USER ROLE")
                .permissions(new HashSet<>())
                .build();
        Permission permission = buildPermission(PermissionCode.USER_READ_SELF);

        when(roleRepository.findWithPermissionsByCode(PredefinedRole.USER)).thenReturn(Optional.of(role));
        when(permissionRepository.findByCodeIn(Set.of(PermissionCode.USER_READ_SELF)))
                .thenReturn(List.of(permission));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = permissionService.updateRolePermissions(
                PredefinedRole.USER,
                UpdateRolePermissionsRequest.builder()
                        .permissions(Set.of(PermissionCode.USER_READ_SELF))
                        .build());

        assertThat(response.getPermissions()).extracting("code").containsExactly(PermissionCode.USER_READ_SELF);
        assertThat(role.getPermissions()).containsExactly(permission);
    }

    @Test
    void updateRolePermissions_shouldNotRemoveRequiredAdminPermissions() {
        Role role = Role.builder()
                .id(1L)
                .code(PredefinedRole.ADMIN)
                .description("ADMIN ROLE")
                .permissions(new HashSet<>())
                .build();

        when(roleRepository.findWithPermissionsByCode(PredefinedRole.ADMIN)).thenReturn(Optional.of(role));

        assertThatThrownBy(() -> permissionService.updateRolePermissions(
                        PredefinedRole.ADMIN,
                        UpdateRolePermissionsRequest.builder()
                                .permissions(Set.of(PermissionCode.ROLE_MANAGE))
                                .build()))
                .isInstanceOf(AppException.class);
    }

    @Test
    void seedDefaultPermissions_shouldCreateMissingPermissionsAndAssignDefaults() {
        Map<PermissionCode, Permission> savedPermissions = new EnumMap<>(PermissionCode.class);
        Role userRole = Role.builder()
                .id(1L)
                .code(PredefinedRole.USER)
                .description("USER ROLE")
                .permissions(new HashSet<>())
                .build();
        Role adminRole = Role.builder()
                .id(3L)
                .code(PredefinedRole.ADMIN)
                .description("ADMIN ROLE")
                .permissions(new HashSet<>())
                .build();

        when(permissionRepository.findAll()).thenReturn(List.of());
        when(permissionRepository.save(any(Permission.class))).thenAnswer(invocation -> {
            Permission permission = invocation.getArgument(0);
            savedPermissions.put(permission.getCode(), permission);
            return permission;
        });
        when(roleRepository.findWithPermissionsByCode(PredefinedRole.USER)).thenReturn(Optional.of(userRole));
        when(roleRepository.findWithPermissionsByCode(PredefinedRole.ADMIN)).thenReturn(Optional.of(adminRole));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        permissionService.seedDefaultPermissions();

        assertThat(savedPermissions).hasSize(PermissionCode.values().length);
        assertThat(userRole.getPermissions()).extracting(Permission::getCode).contains(PermissionCode.USER_READ_SELF);
        assertThat(adminRole.getPermissions())
                .extracting(Permission::getCode)
                .contains(PermissionCode.PERMISSION_MANAGE, PermissionCode.ROLE_MANAGE);
    }

    private Permission buildPermission(PermissionCode code) {
        return Permission.builder()
                .id((long) code.ordinal())
                .code(code)
                .name(code.name())
                .description(code.name())
                .group(code.getGroup())
                .system(true)
                .build();
    }
}
