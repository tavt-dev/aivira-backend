package com.tien.aivirabackend.service.impl;

import com.tien.aivirabackend.domain.dto.request.*;
import com.tien.aivirabackend.domain.dto.response.ActiveSessionResponse;
import com.tien.aivirabackend.constant.OtpType;
import com.tien.aivirabackend.domain.entity.user.UserOtp;
import com.tien.aivirabackend.exception.errorCode.OtpErrorCode;
import com.tien.aivirabackend.repository.UserOtpRepository;
import com.tien.aivirabackend.service.EmailService;
import com.tien.aivirabackend.service.UserOtpService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import lombok.extern.slf4j.Slf4j;

import java.util.List;

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

    UserOtpRepository userOtpRepository;

    EmailService emailService;

    @Override
    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request, String deviceInfo, String ipAddress) {
        User user = userRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND));

        boolean isAuthenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            log.error("Authentication failed for user: {}. Reason: Account is deleted.", request.getUsername());
            throw new AppException(AccountErrorCode.ACCOUNT_DELETED);
        }

        // Check account status
        if (Boolean.TRUE.equals(user.getIsLocked())) {
            log.error("Authentication failed for user: {}. Reason: Account is locked.", request.getUsername());
            throw new AppException(AccountErrorCode.ACCOUNT_LOCKED);
        }

        if (!isAuthenticated) {
            log.error("Authentication failed for user: {}", request.getUsername());
            throw new AppException(PasswordErrorCode.PASSWORD_INCORRECT);
        }

        String accessToken = jwtService.createAccessToken(user);
        String refreshToken = jwtService.createRefreshToken(user, deviceInfo, ipAddress, null);

        log.info("User: {} authenticated successfully.", request.getUsername());

        return AuthenticationResponse.builder()
                .token(accessToken)
                // Todo: Set HttpOnly cookie for refresh token in controller
                .refreshToken(refreshToken)
                .authenticated(true)
                .build();
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

            // Check account status
            if (Boolean.TRUE.equals(user.getIsDeleted())) {
                log.error("Refresh token failed for user: {}. Reason: Account is deleted.", username);
                throw new AppException(AccountErrorCode.ACCOUNT_DELETED);
            }

            if (Boolean.TRUE.equals(user.getIsLocked())) {
                log.error("Refresh token failed for user: {}. Reason: Account is locked.", username);
                throw new AppException(AccountErrorCode.ACCOUNT_LOCKED);
            }

            String familyId = jwtService.getTokenFamilyId(refreshToken);

            // Revoke old refresh token
            jwtService.revokeRefreshToken(refreshToken);

            String newAccessToken = jwtService.createAccessToken(user);
            String newRefreshToken = jwtService.createRefreshToken(user, deviceInfo, ipAddress, familyId);

            log.info("Refresh token successful for user: {}", username);

            return AuthenticationResponse.builder()
                    .token(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .authenticated(true)
                    .build();

        } catch (Exception e) {
            log.error("Failed to refresh token", e);
            throw new AppException(JwtErrorCode.REFRESH_TOKEN_INVALID);
        }
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        jwtService.revokeRefreshToken(refreshToken);
        log.info("User logged out successfully");
    }

    @Override
    @Transactional
    public void logoutAll() {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND));

        jwtService.revokeAllTokensOfUser(user.getId());

        log.info("All sessions logged out successfully for user: {}", username);
    }

    @Override
    public List<ActiveSessionResponse> getActiveSessions(String currentSessionId) {
        return List.of();
    }

    @Override
    public void revokeSession(String sessionId) {

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

        userOtpService.markOtpAsUsed(userOtp);

        log.info("User password reset successfully: {}", request.getEmail());
    }
}
