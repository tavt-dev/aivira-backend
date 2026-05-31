package com.tien.aivirabackend.service.auth.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.tien.aivirabackend.constant.OtpType;
import com.tien.aivirabackend.constant.PredefinedRole;
import com.tien.aivirabackend.constant.RevocationReason;
import com.tien.aivirabackend.constant.SignInProvider;
import com.tien.aivirabackend.domain.dto.request.AuthenticationRequest;
import com.tien.aivirabackend.domain.dto.request.ForgotPasswordRequest;
import com.tien.aivirabackend.domain.dto.request.ResendOtpRequest;
import com.tien.aivirabackend.domain.dto.request.ResetPasswordRequest;
import com.tien.aivirabackend.domain.dto.request.UserRegisterRequest;
import com.tien.aivirabackend.domain.dto.request.VerifyUserRequest;
import com.tien.aivirabackend.domain.dto.response.ActiveSessionResponse;
import com.tien.aivirabackend.domain.dto.response.AuthenticationResponse;
import com.tien.aivirabackend.domain.dto.response.UserResponse;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.entity.user.UserOtp;
import com.tien.aivirabackend.domain.mapper.UserMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.JwtErrorCode;
import com.tien.aivirabackend.exception.errorCode.PasswordErrorCode;
import com.tien.aivirabackend.exception.errorCode.UserErrorCode;
import com.tien.aivirabackend.repository.RoleRepository;
import com.tien.aivirabackend.repository.UserRepository;
import com.tien.aivirabackend.service.auth.AuthenticationService;
import com.tien.aivirabackend.service.auth.CurrentUserService;
import com.tien.aivirabackend.service.notification.EmailService;
import com.tien.aivirabackend.service.auth.JwtService;
import com.tien.aivirabackend.service.auth.UserOtpService;
import com.tien.aivirabackend.service.auth.AccountAuthPolicy;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

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

    CurrentUserService currentUserService;

    AccountAuthPolicy accountAuthPolicy;

    @NonFinal
    @Value("${jwt.valid-duration}")
    long accessTokenExpiresIn;

    @Override
    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request, String deviceInfo, String ipAddress) {
        User user = userRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND));

        accountAuthPolicy.validateAccountForAuth(user);

        boolean isAuthenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!isAuthenticated) {
            accountAuthPolicy.registerFailedLoginAttempt(user, ipAddress, deviceInfo);
            log.warn(
                    "auth_login_failed userId={} username={} ip={} device={} reason=invalid_password",
                    user.getId(),
                    user.getUsername(),
                    ipAddress,
                    deviceInfo);
            throw new AppException(PasswordErrorCode.PASSWORD_INCORRECT);
        }

        accountAuthPolicy.clearFailedLoginState(user);

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

        initializeLocalUser(user, request.getPassword());

        var role = roleRepository
                .findByCode(PredefinedRole.USER)
                .orElseThrow(() -> new AppException(UserErrorCode.ROLE_NOT_FOUND));

        user.getRoles().add(role);

        User savedUser = userRepository.save(user);

        sendRegistrationOtp(savedUser, false);

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

            accountAuthPolicy.validateAccountForAuth(user);

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
        log.info(
                "auth_revoke_session_success userId={} username={} sessionId={}",
                user.getId(),
                user.getUsername(),
                sessionId);
    }

    @Override
    @Transactional
    public void verifyUser(VerifyUserRequest request) {
        User user = findUserByEmail(request.getEmail());

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
        User user = findUserByEmail(request.getEmail());
        ensureUserNeedsVerification(user, request.getEmail());
        sendRegistrationOtp(user, true);

        log.info("Resend verification OTP sent to: {}", request.getEmail());
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = findUserByEmail(request.getEmail());
        ensureEmailVerified(user, request.getEmail(), "send password reset OTP");
        sendPasswordResetOtp(user);

        log.info("Forgot password OTP sent to: {}", request.getEmail());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = findUserByEmail(request.getEmail());
        ensureEmailVerified(user, request.getEmail(), "reset password");

        var userOtp = userOtpService.findLatestOtp(user, OtpType.RESET_PASSWORD);
        userOtpService.validateOtp(userOtp, request.getOtpCode());

        resetPasswordAndRevokeSessions(user, request.getNewPassword(), userOtp);

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

    private void initializeLocalUser(User user, String rawPassword) {
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setProvider(SignInProvider.LOCAL);
        user.setProviderUserId(null);
        user.setEmailVerified(false);
        user.setIsActive(false);
        user.setIsLocked(false);
        user.setIsDeleted(false);
        user.setTokenVersion(0);
        user.setFailedLoginAttempts(0);
        user.setFirstFailedLoginAt(null);
        user.setLockoutUntil(null);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND));
    }

    private void ensureUserNeedsVerification(User user, String email) {
        if (Boolean.TRUE.equals(user.getIsActive()) && Boolean.TRUE.equals(user.getEmailVerified())) {
            log.warn("User {} is already verified and active. No OTP sent.", email);
            throw new AppException(UserErrorCode.USER_ALREADY_VERIFIED);
        }
    }

    private void ensureEmailVerified(User user, String email, String action) {
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            log.warn("User {} email is not verified. Cannot {}.", email, action);
            throw new AppException(UserErrorCode.EMAIL_NOT_VERIFIED);
        }
    }

    private void sendRegistrationOtp(User user, boolean enforceFrequencyLimit) {
        if (enforceFrequencyLimit) {
            userOtpService.checkOtpFrequency(user, OtpType.REGISTER);
        }
        userOtpService.deactivateOldOtps(user.getId(), OtpType.REGISTER);
        UserOtp otp = userOtpService.createOtp(user, OtpType.REGISTER, 10);
        emailService.sendRegistrationOtpByEmail(user.getEmail(), user.getUsername(), otp.getOtpCode());
    }

    private void sendPasswordResetOtp(User user) {
        userOtpService.checkOtpFrequency(user, OtpType.RESET_PASSWORD);
        userOtpService.deactivateOldOtps(user.getId(), OtpType.RESET_PASSWORD);
        UserOtp otp = userOtpService.createOtp(user, OtpType.RESET_PASSWORD, 10);
        emailService.sendForgotPasswordOtpByEmail(user.getEmail(), user.getUsername(), otp.getOtpCode());
    }

    private void resetPasswordAndRevokeSessions(User user, String newPassword, UserOtp userOtp) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        jwtService.revokeAllTokensOfUser(user.getId(), RevocationReason.PASSWORD_CHANGE);
        userOtpService.markOtpAsUsed(userOtp);
    }

    private User getCurrentUserFromSecurityContext() {
        return userRepository
                .findById(currentUserService.getCurrentUserId())
                .orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND_BY_ID));
    }

    private org.springframework.security.oauth2.jwt.Jwt getCurrentJwtFromSecurityContext() {
        return currentUserService.getCurrentJwt();
    }
}
