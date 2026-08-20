package com.tien.aivirabackend.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.tien.aivirabackend.constant.PredefinedRole;
import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.UpdateUserRolesRequest;
import com.tien.aivirabackend.domain.dto.response.AdminUserResponse;
import com.tien.aivirabackend.service.user.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/admin/users")
@Tag(name = "Admin Users")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminUserController {
    UserService userService;

    @GetMapping
    @Operation(summary = "List users for admin")
    @PreAuthorize("@authorizationService.hasAnyPermission('USER_MANAGE_ALL', 'USER_READ_ALL')")
    public ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>> getAdminUsers(
            @RequestParam(required = false) String keyword, @RequestParam(required = false) PredefinedRole role,
            @RequestParam(required = false) Boolean active, @RequestParam(required = false) Boolean locked,
            @RequestParam(required = false) Boolean emailVerified, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("Get admin users successful",
                userService.getAdminUsers(keyword, role, active, locked, emailVerified, page, size)));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get admin user detail")
    @PreAuthorize("@authorizationService.hasAnyPermission('USER_MANAGE_ALL', 'USER_READ_ALL')")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getAdminUser(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success("Get admin user successful", userService.getAdminUser(userId)));
    }

    @PutMapping("/{userId}/lock")
    @Operation(summary = "Lock user account")
    @PreAuthorize("@authorizationService.hasAnyPermission('USER_MANAGE_ALL', 'USER_LOCK')")
    public ResponseEntity<ApiResponse<AdminUserResponse>> lockUser(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success("Lock user successful", userService.lockUser(userId)));
    }

    @PutMapping("/{userId}/unlock")
    @Operation(summary = "Unlock user account")
    @PreAuthorize("@authorizationService.hasAnyPermission('USER_MANAGE_ALL', 'USER_UNLOCK')")
    public ResponseEntity<ApiResponse<AdminUserResponse>> unlockUser(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success("Unlock user successful", userService.unlockUser(userId)));
    }

    @PutMapping("/{userId}/roles")
    @Operation(summary = "Replace user roles")
    @PreAuthorize("@authorizationService.hasAnyPermission('USER_MANAGE_ALL', 'USER_ASSIGN_ROLE')")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUserRoles(@PathVariable String userId,
            @Valid @RequestBody UpdateUserRolesRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("Update user roles successful", userService.updateUserRoles(userId, request)));
    }
}
