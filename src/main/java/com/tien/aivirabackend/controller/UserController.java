package com.tien.aivirabackend.controller;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.ClaimAnonymousHistoryRequest;
import com.tien.aivirabackend.domain.dto.request.UpdatePasswordRequest;
import com.tien.aivirabackend.domain.dto.request.UserUpdateRequest;
import com.tien.aivirabackend.domain.dto.response.ClaimAnonymousHistoryResponse;
import com.tien.aivirabackend.domain.dto.response.RecentlyViewedProductResponse;
import com.tien.aivirabackend.domain.dto.response.UserResponse;
import com.tien.aivirabackend.service.analytics.RecentlyViewedService;
import com.tien.aivirabackend.service.user.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "USER-CONTROLLER")
@RestController
@RequestMapping("/users/me")
@Tag(name = "User Profile", description = "Current-user profile, avatar, password, and account lifecycle APIs")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    UserService userService;
    RecentlyViewedService recentlyViewedService;

    @GetMapping("/recently-viewed")
    @Operation(summary = "Get my recently viewed books")
    public ResponseEntity<ApiResponse<PageResponse<RecentlyViewedProductResponse>>> getRecentlyViewed(
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                "Get recently viewed products successful", recentlyViewedService.getMine(page, size)));
    }

    @DeleteMapping("/recently-viewed/{productId}")
    @Operation(summary = "Remove one book from my recently viewed history")
    public ResponseEntity<ApiResponse<Void>> removeRecentlyViewed(@PathVariable Long productId) {
        recentlyViewedService.remove(productId);
        return ResponseEntity.ok(ApiResponse.success("Remove recently viewed product successful", null));
    }

    @DeleteMapping("/recently-viewed")
    @Operation(summary = "Clear my recently viewed history")
    public ResponseEntity<ApiResponse<Void>> clearRecentlyViewed() {
        recentlyViewedService.clear();
        return ResponseEntity.ok(ApiResponse.success("Clear recently viewed products successful", null));
    }

    @PostMapping("/recently-viewed/claim")
    @Operation(summary = "Claim anonymous product view history")
    public ResponseEntity<ApiResponse<ClaimAnonymousHistoryResponse>> claimRecentlyViewed(
            @Valid @RequestBody ClaimAnonymousHistoryRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Claim anonymous history successful", recentlyViewedService.claim(request)));
    }

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
    @Operation(
            summary = "Change my password",
            description = "Changes the authenticated user's password and revokes existing sessions.")
    public ResponseEntity<ApiResponse<Void>> changeMyPassword(@RequestBody UpdatePasswordRequest request) {
        log.info("change my password request");
        userService.changeMyPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Change password successful", null));
    }

    @PostMapping("/deactivate")
    @Operation(
            summary = "Deactivate my account",
            description = "Marks the authenticated user's account inactive/deleted and revokes sessions.")
    public ResponseEntity<ApiResponse<Void>> requestDeactivateMyAccount() {
        log.info("request deactivate my account");
        userService.requestDeactivateMyAccount();
        return ResponseEntity.ok(ApiResponse.success("Deactivate account request successful", null));
    }
}
