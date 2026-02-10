package com.tien.aivirabackend.service.impl;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.tien.aivirabackend.constant.PredefinedRole;
import com.tien.aivirabackend.constant.SignInProvider;
import com.tien.aivirabackend.domain.dto.request.AuthenticationRequest;
import com.tien.aivirabackend.domain.dto.request.UserRegisterRequest;
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
        String refreshToken = jwtService.createRefreshToken(user, deviceInfo, ipAddress. mull);

        log.info("User: {} authenticated successfully.", request.getUsername());

        return AuthenticationResponse.builder()
                .token(accessToken)
                // Todo: Set HttpOnly cookie for refresh token in controller
                .refreshToken(refreshToken)
                .authenticated(true)
                .build();
    }

    @Override
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

        log.info("User registered successfully. Username: {}", savedUser.getUsername());

        return userMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public AuthenticationResponse refreshToken(String refreshToken) {
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

            // Revoke old refresh token
            jwtService.revokeRefreshToken(refreshToken);

            String newAccessToken = jwtService.createAccessToken(user);
            String newRefreshToken = jwtService.createRefreshToken(user);
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
    public void logoutAllSessions() {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND));

        jwtService.revokeAllTokensOfUser(user.getId());

        log.info("All sessions logged out successfully for user: {}", username);
    }
}
