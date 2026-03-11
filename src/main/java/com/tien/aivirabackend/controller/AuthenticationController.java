package com.tien.aivirabackend.controller;

import com.tien.aivirabackend.domain.dto.request.*;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.domain.dto.response.AuthenticationResponse;
import com.tien.aivirabackend.domain.dto.response.UserResponse;
import com.tien.aivirabackend.service.AuthenticationService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "AUTHENTICATION-CONTROLLER")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    AuthenticationService authenticationService;

    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<AuthenticationResponse>> authenticate(
            @Valid @RequestBody AuthenticationRequest request,
            HttpServletRequest httpServlet) {

        log.info("Authenticate request: username={}", request.getUsername());

        String deviceInfo = httpServlet.getHeader("User-Agent");
        String ipAddress = httpServlet.getRemoteAddr();

        AuthenticationResponse response = authenticationService.authenticate(request, deviceInfo, ipAddress);
        return ResponseEntity.ok(ApiResponse.success("Authentication successful", response));
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody UserRegisterRequest request) {
        log.info("Register request: email={}", request.getEmail());

        UserResponse response = authenticationService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Register successful", response));
    }

    @PostMapping(value = "/refresh-token", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<AuthenticationResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpServlet) {

        log.info("Refresh token request");

        String deviceInfo = httpServlet.getHeader("User-Agent");
        String ipAddress = httpServlet.getRemoteAddr();

        AuthenticationResponse response = authenticationService.refreshToken(request.getRefreshToken(), deviceInfo, ipAddress);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping(value = "/logout", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequest request) {
        log.info("Logout request");

        authenticationService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    @PostMapping(value = "logout-all")
    public ResponseEntity<ApiResponse<Void>> logoutAll() {
        log.info("Logout all request");
        authenticationService.logoutAll();
        return ResponseEntity.ok(ApiResponse.success("Logout all successful", null));
    }

    @PostMapping(value = "verify-user")
    public  ResponseEntity<ApiResponse<Void>> verifyUser(@RequestBody VerifyUserRequest request) {
        log.info("Verify user request: email={}", request.getEmail());
        authenticationService.verifyUser(request);
        return ResponseEntity.ok(ApiResponse.success("User verified successfully", null));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerificationEmail(@RequestBody ResendOtpRequest request) {
        log.info("Resend verification email request: email={}", request.getEmail());
        authenticationService.resendVerificationOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Verification email resent successfully", null));
    }

    @PostMapping("forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        log.info("Forgot password request: email={}", request.getEmail());
        authenticationService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset OTP sent successfully", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody ResetPasswordRequest request) {
        log.info("Reset password request: email={}", request.getEmail());
        authenticationService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset email sent successfully", null));
    }

}
