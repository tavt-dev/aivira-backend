package com.tien.aivirabackend.controller;

import java.util.List;

import com.tien.aivirabackend.constant.PredefinedRole;
import com.tien.aivirabackend.constant.PermissionCode;
import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.domain.dto.request.GrantUserPermissionRequest;
import com.tien.aivirabackend.domain.dto.request.UpdateRolePermissionsRequest;
import com.tien.aivirabackend.domain.dto.response.PermissionResponse;
import com.tien.aivirabackend.domain.dto.response.RolePermissionResponse;
import com.tien.aivirabackend.domain.dto.response.UserEffectivePermissionsResponse;
import com.tien.aivirabackend.domain.dto.response.UserPermissionResponse;
import com.tien.aivirabackend.service.PermissionService;
import com.tien.aivirabackend.service.UserPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j(topic = "ADMIN-PERMISSION-CONTROLLER")
@RestController
@RequestMapping("/admin")
@Tag(name = "Admin Permissions", description = "Role and permission administration APIs")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminPermissionController {
    PermissionService permissionService;
    UserPermissionService userPermissionService;

    @GetMapping("/permissions")
    @Operation(summary = "List permissions", description = "Returns all system permissions.")
    @PreAuthorize("@authorizationService.hasAnyPermission('PERMISSION_MANAGE', 'ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getPermissions() {
        log.info("Get permissions request");
        return ResponseEntity.ok(ApiResponse.success(
                "Get permissions successful", permissionService.getAllPermissions()));
    }

    @GetMapping("/roles")
    @Operation(summary = "List roles", description = "Returns all roles with assigned permissions.")
    @PreAuthorize("@authorizationService.hasAnyPermission('PERMISSION_MANAGE', 'ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<List<RolePermissionResponse>>> getRoles() {
        log.info("Get roles request");
        return ResponseEntity.ok(ApiResponse.success(
                "Get roles successful", permissionService.getAllRolesWithPermissions()));
    }

    @GetMapping("/roles/{roleCode}/permissions")
    @Operation(summary = "Get role permissions", description = "Returns permissions assigned to one role.")
    @PreAuthorize("@authorizationService.hasAnyPermission('PERMISSION_MANAGE', 'ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<RolePermissionResponse>> getRolePermissions(@PathVariable PredefinedRole roleCode) {
        log.info("Get role permissions request: roleCode={}", roleCode);
        return ResponseEntity.ok(ApiResponse.success(
                "Get role permissions successful", permissionService.getRolePermissions(roleCode)));
    }

    @PutMapping("/roles/{roleCode}/permissions")
    @Operation(summary = "Update role permissions", description = "Replaces permissions assigned to one role.")
    @PreAuthorize("@authorizationService.hasAnyPermission('PERMISSION_MANAGE', 'ROLE_MANAGE')")
    public ResponseEntity<ApiResponse<RolePermissionResponse>> updateRolePermissions(
            @PathVariable PredefinedRole roleCode, @Valid @RequestBody UpdateRolePermissionsRequest request) {
        log.info("Update role permissions request: roleCode={}", roleCode);
        return ResponseEntity.ok(ApiResponse.success(
                "Update role permissions successful", permissionService.updateRolePermissions(roleCode, request)));
    }

    @GetMapping("/users/{userId}/permissions")
    @Operation(summary = "Get user effective permissions", description = "Returns role, direct, and effective permissions for one user.")
    @PreAuthorize("@authorizationService.hasAnyPermission('PERMISSION_MANAGE', 'USER_PERMISSION_MANAGE', 'USER_PERMISSION_READ')")
    public ResponseEntity<ApiResponse<UserEffectivePermissionsResponse>> getUserPermissions(@PathVariable String userId) {
        log.info("Get user permissions request: userId={}", userId);
        return ResponseEntity.ok(ApiResponse.success(
                "Get user permissions successful", userPermissionService.getUserPermissions(userId)));
    }

    @PostMapping("/users/{userId}/permissions")
    @Operation(summary = "Grant direct user permission", description = "Assigns one direct permission to one user.")
    @PreAuthorize("@authorizationService.hasAnyPermission('PERMISSION_MANAGE', 'USER_PERMISSION_MANAGE', 'USER_PERMISSION_GRANT')")
    public ResponseEntity<ApiResponse<UserPermissionResponse>> grantUserPermission(
            @PathVariable String userId, @Valid @RequestBody GrantUserPermissionRequest request) {
        log.info("Grant user permission request: userId={} permission={}", userId, request.getPermissionCode());
        return ResponseEntity.ok(ApiResponse.success(
                "Grant user permission successful", userPermissionService.grantPermission(userId, request)));
    }

    @DeleteMapping("/users/{userId}/permissions/{permissionCode}")
    @Operation(summary = "Revoke direct user permission", description = "Revokes one active direct permission from one user.")
    @PreAuthorize("@authorizationService.hasAnyPermission('PERMISSION_MANAGE', 'USER_PERMISSION_MANAGE', 'USER_PERMISSION_REVOKE')")
    public ResponseEntity<ApiResponse<Void>> revokeUserPermission(
            @PathVariable String userId, @PathVariable PermissionCode permissionCode) {
        log.info("Revoke user permission request: userId={} permission={}", userId, permissionCode);
        userPermissionService.revokePermission(userId, permissionCode);
        return ResponseEntity.ok(ApiResponse.success("Revoke user permission successful", null));
    }
}
