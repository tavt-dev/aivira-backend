package com.tien.aivirabackend.controller;

import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.domain.dto.request.GoogleLoginTicketExchangeRequest;
import com.tien.aivirabackend.domain.dto.response.AuthenticationResponse;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.service.auth.GoogleOAuthService;
import com.tien.aivirabackend.service.auth.RefreshTokenCookieService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "GOOGLE-OAUTH-CONTROLLER")
@RestController
@RequestMapping("/auth/google")
@Tag(name = "Google OAuth", description = "Google OAuth authorization-code login endpoints")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GoogleOAuthController {
    GoogleOAuthService googleOAuthService;
    RefreshTokenCookieService refreshTokenCookieService;

    @NonFinal
    @Value("${auth.refresh-token.body-enabled:true}")
    boolean refreshTokenBodyEnabled;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    long refreshTokenDuration;

    @GetMapping("/authorize")
    @Operation(summary = "Start Google OAuth login", description = "Redirects the browser to Google authorization.")
    public ResponseEntity<Void> authorize(@RequestParam(required = false) String next, HttpServletRequest request) {
        var authorization =
                googleOAuthService.createAuthorization(next, request.getHeader("User-Agent"), request.getRemoteAddr());
        return redirect(authorization.getAuthorizationUrl());
    }

    @GetMapping("/callback")
    @Operation(
            summary = "Handle Google OAuth callback",
            description = "Consumes Google authorization code and redirects to frontend with a one-time ticket.")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpServletRequest request) {
        if (StringUtils.hasText(error)) {
            log.warn("Google OAuth callback returned error={}", error);
            return redirect(googleOAuthService.failureRedirectUrl(error));
        }
        try {
            var callback = googleOAuthService.handleCallback(
                    code, state, request.getHeader("User-Agent"), request.getRemoteAddr());
            return redirect(callback.getRedirectUrl());
        } catch (AppException e) {
            return redirect(
                    googleOAuthService.failureRedirectUrl(e.getErrorCode().getCode()));
        }
    }

    @PostMapping("/exchange-ticket")
    @Operation(
            summary = "Exchange Google login ticket",
            description = "Exchanges a one-time Google login ticket for Aivira JWT and refresh-token cookie.")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> exchangeTicket(
            @Valid @RequestBody GoogleLoginTicketExchangeRequest request,
            HttpServletRequest httpServlet,
            HttpServletResponse httpServletResponse) {
        AuthenticationResponse response = googleOAuthService.exchangeTicket(
                request.getTicket(), httpServlet.getHeader("User-Agent"), httpServlet.getRemoteAddr());
        refreshTokenCookieService.writeRefreshTokenCookie(
                httpServletResponse, response.getRefreshToken(), refreshTokenDuration);
        hideRefreshTokenWhenLegacyBodyDisabled(response);
        return ResponseEntity.ok(ApiResponse.success("Google login successful", response));
    }

    private ResponseEntity<Void> redirect(String redirectUrl) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, URI.create(redirectUrl).toString())
                .build();
    }

    private void hideRefreshTokenWhenLegacyBodyDisabled(AuthenticationResponse response) {
        if (!refreshTokenBodyEnabled) {
            response.setRefreshToken(null);
        }
    }
}
