package com.tien.aivirabackend.controller;

import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.domain.dto.request.UpdatePasswordRequest;
import com.tien.aivirabackend.domain.dto.request.UserUpdateRequest;
import com.tien.aivirabackend.domain.dto.response.UserResponse;
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
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile() {
        log.info("get my profile request");
        UserResponse response = userService.getMyProfile();
        return ResponseEntity.ok(ApiResponse.success("Get profile successful", response));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(@RequestBody UserUpdateRequest request) {
        log.info("update my profile request");
        UserResponse response = userService.updateMyProfile(request);
        return ResponseEntity.ok(ApiResponse.success("Update profile successful", response));
    }

    @PutMapping("/avatar")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyAvatar(@RequestParam("avatar") MultipartFile avatarFile) {
        log.info("update my avatar request");
        UserResponse response = userService.updateMyAvatar(avatarFile);
        return ResponseEntity.ok(ApiResponse.success("Update avatar successful", response));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changeMyPassword(@RequestBody UpdatePasswordRequest request) {
        log.info("change my password request");
        userService.changeMyPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Change password successful", null));
    }

    @PostMapping("/deactivate")
    public ResponseEntity<ApiResponse<Void>> requestDeactivateMyAccount() {
        log.info("request deactivate my account");
        userService.requestDeactivateMyAccount();
        return ResponseEntity.ok(ApiResponse.success("Deactivate account request successful", null));
    }
}
