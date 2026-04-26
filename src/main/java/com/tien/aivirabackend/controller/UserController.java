package com.tien.aivirabackend.controller;

import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.domain.dto.request.UpdatePasswordRequest;
import com.tien.aivirabackend.domain.dto.request.UserUpdateRequest;
import com.tien.aivirabackend.domain.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tien.aivirabackend.service.UserService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

@Slf4j(topic = "USER-CONTROLLER")
@RestController
@RequestMapping("/users/me")
@Tag(name = "User Profile", description = "Current-user profile, avatar, password, and account lifecycle APIs")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    UserService userService;

    @GetMapping
    @Operation(summary = "Get my profile", description = "Returns the authenticated user's profile.")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile() {
        log.info("get my profile request");
        UserResponse response = userService.getMyProfile();
        return ResponseEntity.ok(ApiResponse.success("Get profile successful", response));
    }

    @PutMapping
    @Operation(summary = "Update my profile", description = "Updates profile fields for the authenticated user.")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(@RequestBody UserUpdateRequest request) {
        log.info("update my profile request");
        UserResponse response = userService.updateMyProfile(request);
        return ResponseEntity.ok(ApiResponse.success("Update profile successful", response));
    }

    @PutMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update my avatar", description = "Uploads an avatar image for the authenticated user.")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyAvatar(
            @Parameter(description = "Avatar image file") @RequestParam("avatar") MultipartFile avatarFile) {
        log.info("update my avatar request");
        UserResponse response = userService.updateMyAvatar(avatarFile);
        return ResponseEntity.ok(ApiResponse.success("Update avatar successful", response));
    }

    @PutMapping("/password")
    @Operation(summary = "Change my password", description = "Changes the authenticated user's password and revokes existing sessions.")
    public ResponseEntity<ApiResponse<Void>> changeMyPassword(@RequestBody UpdatePasswordRequest request) {
        log.info("change my password request");
        userService.changeMyPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Change password successful", null));
    }

    @PostMapping("/deactivate")
    @Operation(summary = "Deactivate my account", description = "Marks the authenticated user's account inactive/deleted and revokes sessions.")
    public ResponseEntity<ApiResponse<Void>> requestDeactivateMyAccount() {
        log.info("request deactivate my account");
        userService.requestDeactivateMyAccount();
        return ResponseEntity.ok(ApiResponse.success("Deactivate account request successful", null));
    }
}
