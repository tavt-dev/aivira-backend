package com.tien.aivirabackend.service.impl;

import com.tien.aivirabackend.domain.dto.request.*;
import com.tien.aivirabackend.domain.dto.response.ActiveSessionResponse;
import com.tien.aivirabackend.constant.OtpType;
import com.tien.aivirabackend.constant.RevocationReason;
import com.tien.aivirabackend.domain.entity.user.UserOtp;
import com.tien.aivirabackend.service.EmailService;
import com.tien.aivirabackend.service.UserOtpService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.tien.aivirabackend.constant.PredefinedRole;
import com.tien.aivirabackend.constant.SignInProvider;
import com.tien.aivirabackend.domain.dto.response.AuthenticationResponse;
import com.tien.aivirabackend.domain.dto.response.UserResponse;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.mapper.UserMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.*;
import com.tien.aivirabackend.repository.RoleRepository;
import com.tien.aivirabackend.repository.UserRepository;
import com.tien.aivirabackend.service.AuthenticationService;
import com.tien.aivirabackend.service.JwtService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;

@Service
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Slf4j(topic = "AUTHENTICATION-SERVICE")
public class AuthenticationServiceImpl implements AuthenticationService {

    PasswordEncoder passwordEncoder;

    UserRepository userRepository;

    RoleRepository roleRepository;

    UserMapper userMapper;

    JwtService jwtService;

    UserOtpService userOtpService;

    EmailService emailService;

    @NonFinal
    @Value("${auth.brute-force.max-attempts:5}")
    int maxFailedLoginAttempts;

    @NonFinal
    @Value("${auth.brute-force.window-minutes:15}")
    int failedLoginWindowMinutes;

    @NonFinal
    @Value("${auth.brute-force.lock-minutes:15}")
    int lockMinutes;

    @NonFinal
    @Value("${jwt.valid-duration}")
    long accessTokenExpiresIn;

    @Override
    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request, String deviceInfo, String ipAddress) {
        User user = userRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND));

        validateAccountForAuth(user);

        boolean isAuthenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!isAuthenticated) {
            registerFailedLoginAttempt(user, ipAddress, deviceInfo);
            log.warn(
                    "auth_login_failed userId={} username={} ip={} device={} reason=invalid_password",
                    user.getId(),
                    user.getUsername(),
                    ipAddress,
                    deviceInfo);
            throw new AppException(PasswordErrorCode.PASSWORD_INCORRECT);
        }

        clearFailedLoginState(user);

        String accessToken = jwtService.createAccessToken(user);
        String refreshToken = jwtService.createRefreshToken(user, deviceInfo, ipAddress, null);

        log.info(
                "auth_login_success userId={} username={} ip={} device={}",
                user.getId(),
                user.getUsername(),
                ipAddress,
                deviceInfo);

        return buildAuthenticationResponse(accessToken, refreshToken);
    }

    @Override
    @Transactional
    public UserResponse register(UserRegisterRequest request) {
        // Validate
        if (userRepository.existsByUsername(request.getUsername())) {
            log.error("Đăng kí thất bại. Username: {} đã được sử dụng.", request.getUsername());
            throw new AppException(UserErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            log.error("Đăng kí thất bại. Email: {} đã được sử dụng.", request.getEmail());
            throw new AppException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = userMapper.toUser(request);

        // Set auth
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setProvider(SignInProvider.LOCAL);
        user.setProviderUserId(null);

        // Default status
        user.setEmailVerified(false);
        user.setIsActive(false);
        user.setIsLocked(false);
        user.setIsDeleted(false);
        user.setTokenVersion(0);
        user.setFailedLoginAttempts(0);
        user.setFirstFailedLoginAt(null);
        user.setLockoutUntil(null);

        var role = roleRepository
                .findByCode(PredefinedRole.USER)
                .orElseThrow(() -> new AppException(UserErrorCode.ROLE_NOT_FOUND));

        user.getRoles().add(role);

        User savedUser = userRepository.save(user);

        // Create and send registration OTP
        userOtpService.deactivateOldOtps(savedUser.getId(), OtpType.REGISTER);
        UserOtp otp = userOtpService.createOtp(savedUser, OtpType.REGISTER, 10);
        emailService.sendRegistrationOtpByEmail(savedUser.getEmail(), savedUser.getUsername(), otp.getOtpCode());

        log.info("User registered successfully. Username: {}", savedUser.getUsername());

        return userMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public AuthenticationResponse refreshToken(String refreshToken, String deviceInfo, String ipAddress) {
        SignedJWT signedJWT = jwtService.verifyRefreshToken(refreshToken);

        try {
            JWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();
            String username = jwtClaimsSet.getSubject();

            User user = userRepository
                    .findByUsername(username)
                    .orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND));

            validateAccountForAuth(user);

            String familyId = jwtService.getTokenFamilyId(refreshToken);

            String newAccessToken = jwtService.createAccessToken(user);
            String newRefreshToken = jwtService.createRefreshToken(user, deviceInfo, ipAddress, familyId);
            String replacementJti = jwtService.getTokenJti(newRefreshToken);

            jwtService.revokeRefreshToken(refreshToken, RevocationReason.TOKEN_REFRESH, replacementJti);

            log.info(
                    "auth_refresh_success userId={} username={} ip={} device={} replacementJti={}",
                    user.getId(),
                    username,
                    ipAddress,
                    deviceInfo,
                    replacementJti);

            return buildAuthenticationResponse(newAccessToken, newRefreshToken);

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to refresh token", e);
            throw new AppException(JwtErrorCode.REFRESH_TOKEN_INVALID);
        }
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        String tokenJti = jwtService.getTokenJti(refreshToken);
        jwtService.revokeRefreshToken(refreshToken, RevocationReason.USER_LOGOUT, null);
        log.info("auth_logout_success tokenJti={}", tokenJti);
    }

    @Override
    @Transactional
    public void logoutAll() {
        User user = getCurrentUserFromSecurityContext();
        jwtService.revokeAllTokensOfUser(user.getId(), RevocationReason.USER_LOGOUT_ALL);
        log.info("auth_logout_all_success userId={} username={}", user.getId(), user.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActiveSessionResponse> getActiveSessions() {
        User user = getCurrentUserFromSecurityContext();
        String currentSessionJti = getCurrentJwtFromSecurityContext().getId();
        List<ActiveSessionResponse> sessions = jwtService.getActiveSessions(user.getId(), currentSessionJti);

        log.info(
                "auth_get_sessions userId={} username={} activeSessionCount={}",
                user.getId(),
                user.getUsername(),
                sessions.size());
        return sessions;
    }

    @Override
    @Transactional
    public void revokeSession(String sessionId) {
        User user = getCurrentUserFromSecurityContext();
        jwtService.revokeSession(user.getId(), sessionId);
        log.info("auth_revoke_session_success userId={} username={} sessionId={}", user.getId(), user.getUsername(), sessionId);
    }

    @Override
    @Transactional
    public void verifyUser(VerifyUserRequest request) {
        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND));

        var userOtp = userOtpService.findLatestOtp(user, OtpType.REGISTER);

        userOtpService.validateOtp(userOtp, request.getOtpCode());

        user.setEmailVerified(true);
        user.setIsActive(true);
        userRepository.save(user);

        log.info("User email verified successfully: {}", request.getEmail());
    }

    @Override
    @Transactional
    public void resendVerificationOtp(ResendOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND));
        if(user.getIsActive() && user.getEmailVerified()) {
            log.warn("User: {} is already verified and active. No OTP sent.", request.getEmail());
            throw new AppException(UserErrorCode.USER_ALREADY_VERIFIED);
        }

        userOtpService.checkOtpFrequency(user, OtpType.REGISTER);
        userOtpService.deactivateOldOtps(user.getId(), OtpType.REGISTER);

        UserOtp newOtp = userOtpService.createOtp(user, OtpType.REGISTER, 10);
        emailService.sendRegistrationOtpByEmail(user.getEmail(), user.getUsername(), newOtp.getOtpCode());

        log.info("Resend verification OTP sent to: {}", request.getEmail());
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND));

        if(!user.getEmailVerified()) {
            log.warn("User: {} email is not verified. Cannot send password reset OTP.", request.getEmail());
            throw new AppException(UserErrorCode.EMAIL_NOT_VERIFIED);
        }

        userOtpService.checkOtpFrequency(user, OtpType.RESET_PASSWORD);
        userOtpService.deactivateOldOtps(user.getId(), OtpType.RESET_PASSWORD);

        UserOtp newOtp = userOtpService.createOtp(user, OtpType.RESET_PASSWORD, 10);
        emailService.sendForgotPasswordOtpByEmail(user.getEmail(), user.getUsername(), newOtp.getOtpCode());

        log.info("Forgot password OTP sent to: {}", request.getEmail());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND));

        if(!user.getEmailVerified()) {
            log.warn("User: {} email is not verified. Cannot reset password.", request.getEmail());
            throw new AppException(UserErrorCode.EMAIL_NOT_VERIFIED);
        }

        var userOtp = userOtpService.findLatestOtp(user, OtpType.RESET_PASSWORD);
        userOtpService.validateOtp(userOtp, request.getOtpCode());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        jwtService.revokeAllTokensOfUser(user.getId(), RevocationReason.PASSWORD_CHANGE);
        userOtpService.markOtpAsUsed(userOtp);

        log.info(
                "auth_reset_password_success userId={} username={} email={}",
                user.getId(),
                user.getUsername(),
                request.getEmail());
    }

    private AuthenticationResponse buildAuthenticationResponse(String accessToken, String refreshToken) {
        return AuthenticationResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .accessTokenExpiresIn(accessTokenExpiresIn)
                .authenticated(true)
                .build();
    }

    private void validateAccountForAuth(User user) {
        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new AppException(AccountErrorCode.ACCOUNT_DELETED);
        }

        if (Boolean.TRUE.equals(user.getIsLocked())) {
            throw new AppException(AccountErrorCode.ACCOUNT_LOCKED);
        }

        Instant now = Instant.now();
        if (user.getLockoutUntil() != null && user.getLockoutUntil().isAfter(now)) {
            throw new AppException(AccountErrorCode.ACCOUNT_LOCKED);
        }

        if (user.getLockoutUntil() != null && !user.getLockoutUntil().isAfter(now)) {
            user.setLockoutUntil(null);
            user.setFailedLoginAttempts(0);
            user.setFirstFailedLoginAt(null);
            userRepository.save(user);
        }

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new AppException(AccountErrorCode.ACCOUNT_NOT_VERIFIED);
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AppException(UserErrorCode.USER_ACCOUNT_INACTIVE);
        }
    }

    private void registerFailedLoginAttempt(User user, String ipAddress, String deviceInfo) {
        Instant now = Instant.now();
        Instant boundary = now.minus(failedLoginWindowMinutes, ChronoUnit.MINUTES);

        Integer currentAttempts = Objects.requireNonNullElse(user.getFailedLoginAttempts(), 0);
        Instant firstFailedAt = user.getFirstFailedLoginAt();

        if (firstFailedAt == null || firstFailedAt.isBefore(boundary)) {
            firstFailedAt = now;
            currentAttempts = 0;
        }

        int newAttempts = currentAttempts + 1;
        user.setFirstFailedLoginAt(firstFailedAt);
        user.setFailedLoginAttempts(newAttempts);

        if (newAttempts >= maxFailedLoginAttempts) {
            user.setLockoutUntil(now.plus(lockMinutes, ChronoUnit.MINUTES));
            user.setFailedLoginAttempts(0);
            user.setFirstFailedLoginAt(null);
            log.warn(
                    "auth_login_lockout userId={} username={} ip={} device={} lockUntil={}",
                    user.getId(),
                    user.getUsername(),
                    ipAddress,
                    deviceInfo,
                    user.getLockoutUntil());
        }

        userRepository.save(user);
    }

    private void clearFailedLoginState(User user) {
        boolean changed = false;
        if (!Objects.equals(user.getFailedLoginAttempts(), 0)) {
            user.setFailedLoginAttempts(0);
            changed = true;
        }
        if (user.getFirstFailedLoginAt() != null) {
            user.setFirstFailedLoginAt(null);
            changed = true;
        }
        if (user.getLockoutUntil() != null) {
            user.setLockoutUntil(null);
            changed = true;
        }
        if (changed) {
            userRepository.save(user);
        }
    }

    private User getCurrentUserFromSecurityContext() {
        Jwt jwt = getCurrentJwtFromSecurityContext();
        String userId = jwt.getClaimAsString("user_id");

        if (userId == null || userId.isBlank()) {
            throw new AppException(AuthErrorCode.AUTHENTICATION_FAILED);
        }

        return userRepository.findById(userId).orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND_BY_ID));
    }

    private Jwt getCurrentJwtFromSecurityContext() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(AuthErrorCode.AUTHENTICATION_FAILED);
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            throw new AppException(AuthErrorCode.AUTHENTICATION_FAILED);
        }
        return jwt;
    }
}
