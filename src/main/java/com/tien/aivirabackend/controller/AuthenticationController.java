package com.tien.aivirabackend.controller;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.domain.dto.request.AuthenticationRequest;
import com.tien.aivirabackend.domain.dto.request.ForgotPasswordRequest;
import com.tien.aivirabackend.domain.dto.request.LogoutRequest;
import com.tien.aivirabackend.domain.dto.request.RefreshTokenRequest;
import com.tien.aivirabackend.domain.dto.request.ResendOtpRequest;
import com.tien.aivirabackend.domain.dto.request.ResetPasswordRequest;
import com.tien.aivirabackend.domain.dto.request.UserRegisterRequest;
import com.tien.aivirabackend.domain.dto.request.VerifyUserRequest;
import com.tien.aivirabackend.domain.dto.response.ActiveSessionResponse;
import com.tien.aivirabackend.domain.dto.response.AuthenticationResponse;
import com.tien.aivirabackend.domain.dto.response.UserResponse;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.JwtErrorCode;
import com.tien.aivirabackend.service.auth.AuthenticationService;
import com.tien.aivirabackend.service.auth.RefreshTokenCookieService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "AUTHENTICATION-CONTROLLER")
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Authentication, token lifecycle, OTP, and session APIs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    AuthenticationService authenticationService;
    RefreshTokenCookieService refreshTokenCookieService;

    @NonFinal
    @Value("${auth.refresh-token.body-enabled:true}")
    boolean refreshTokenBodyEnabled;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    long refreshTokenDuration;

    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Login",
            description = "Authenticates a verified local user and returns access and refresh tokens.")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> authenticate(
            @Valid @RequestBody AuthenticationRequest request,
            @Parameter(hidden = true) HttpServletRequest httpServlet,
            @Parameter(hidden = true) HttpServletResponse httpServletResponse) {

        log.info("Authenticate request: username={}", request.getUsername());

        RequestMetadata metadata = resolveRequestMetadata(httpServlet);

        AuthenticationResponse response =
                authenticationService.authenticate(request, metadata.deviceInfo(), metadata.ipAddress());

        refreshTokenCookieService.writeRefreshTokenCookie(
                httpServletResponse, response.getRefreshToken(), refreshTokenDuration);
        hideRefreshTokenWhenLegacyBodyDisabled(response);

        return ResponseEntity.ok(ApiResponse.success("Authentication successful", response));
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Register user", description = "Creates a local user and sends a registration OTP to email.")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody UserRegisterRequest request) {
        log.info("Register request: email={}", request.getEmail());

        UserResponse response = authenticationService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Register successful", response));
    }

    @PostMapping(value = "/refresh-token")
    @Operation(
            summary = "Refresh token",
            description =
                    "Rotates the refresh token and returns a new access token. Refresh token can be supplied by cookie or request body.")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> refreshToken(
            @RequestBody(required = false) RefreshTokenRequest request,
            @Parameter(hidden = true) HttpServletRequest httpServlet,
            @Parameter(hidden = true) HttpServletResponse httpServletResponse) {

        log.info("Refresh token request");

        String requestRefreshToken = request == null ? null : request.getRefreshToken();
        String refreshToken = resolveRefreshToken(httpServlet, requestRefreshToken);

        if (!StringUtils.hasText(refreshToken)) {
            throw new AppException(JwtErrorCode.TOKEN_MISSING);
        }

        RequestMetadata metadata = resolveRequestMetadata(httpServlet);

        AuthenticationResponse response =
                authenticationService.refreshToken(refreshToken, metadata.deviceInfo(), metadata.ipAddress());

        refreshTokenCookieService.writeRefreshTokenCookie(
                httpServletResponse, response.getRefreshToken(), refreshTokenDuration);
        hideRefreshTokenWhenLegacyBodyDisabled(response);

        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping(value = "/logout")
    @Operation(
            summary = "Logout",
            description = "Revokes the supplied refresh token and clears the refresh-token cookie.")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) LogoutRequest request,
            @Parameter(hidden = true) HttpServletRequest httpServlet,
            @Parameter(hidden = true) HttpServletResponse httpServletResponse) {
        log.info("Logout request");

        String requestRefreshToken = request == null ? null : request.getRefreshToken();
        String refreshToken = resolveRefreshToken(httpServlet, requestRefreshToken);

        if (!StringUtils.hasText(refreshToken)) {
            throw new AppException(JwtErrorCode.TOKEN_MISSING);
        }

        authenticationService.logout(refreshToken);
        refreshTokenCookieService.clearRefreshTokenCookie(httpServletResponse);

        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    @PostMapping(value = "/logout-all")
    @Operation(
            summary = "Logout all sessions",
            description = "Revokes all refresh-token sessions for the current user.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> logoutAll(
            @Parameter(hidden = true) HttpServletResponse httpServletResponse) {
        log.info("Logout all request");
        authenticationService.logoutAll();
        refreshTokenCookieService.clearRefreshTokenCookie(httpServletResponse);
        return ResponseEntity.ok(ApiResponse.success("Logout all successful", null));
    }

    @GetMapping("/sessions")
    @Operation(
            summary = "Get active sessions",
            description = "Lists active refresh-token sessions for the current user.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<List<ActiveSessionResponse>>> getActiveSessions() {
        log.info("Get active sessions request");
        return ResponseEntity.ok(
                ApiResponse.success("Get active sessions successful", authenticationService.getActiveSessions()));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Revoke session", description = "Revokes one active refresh-token session by session ID.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> revokeSession(
            @Parameter(description = "Session ID returned by the active sessions API") @PathVariable String sessionId) {
        log.info("Revoke session request: sessionId={}", sessionId);
        authenticationService.revokeSession(sessionId);
        return ResponseEntity.ok(ApiResponse.success("Session revoked successfully", null));
    }

    @PostMapping(value = "/verify-user")
    @Operation(summary = "Verify user", description = "Verifies a newly registered user using the email OTP.")
    public ResponseEntity<ApiResponse<Void>> verifyUser(@Valid @RequestBody VerifyUserRequest request) {
        log.info("Verify user request: email={}", request.getEmail());
        authenticationService.verifyUser(request);
        return ResponseEntity.ok(ApiResponse.success("User verified successfully", null));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend verification OTP", description = "Sends a new registration OTP to an unverified user.")
    public ResponseEntity<ApiResponse<Void>> resendVerificationEmail(@Valid @RequestBody ResendOtpRequest request) {
        log.info("Resend verification email request: email={}", request.getEmail());
        authenticationService.resendVerificationOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Verification email resent successfully", null));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password", description = "Sends a password-reset OTP to a verified user's email.")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Forgot password request: email={}", request.getEmail());
        authenticationService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset OTP sent successfully", null));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Resets the user's password using the password-reset OTP.")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Reset password request: email={}", request.getEmail());
        authenticationService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successful", null));
    }

    private RequestMetadata resolveRequestMetadata(HttpServletRequest request) {
        return new RequestMetadata(request.getHeader("User-Agent"), request.getRemoteAddr());
    }

    private String resolveRefreshToken(HttpServletRequest request, String requestRefreshToken) {
        String refreshTokenFromCookie = refreshTokenCookieService.extractRefreshToken(request);
        if (StringUtils.hasText(refreshTokenFromCookie)) {
            return refreshTokenFromCookie;
        }

        if (refreshTokenBodyEnabled && StringUtils.hasText(requestRefreshToken)) {
            return requestRefreshToken;
        }

        return null;
    }

    private void hideRefreshTokenWhenLegacyBodyDisabled(AuthenticationResponse response) {
        if (!refreshTokenBodyEnabled) {
            response.setRefreshToken(null);
        }
    }

    private record RequestMetadata(String deviceInfo, String ipAddress) {}
}
