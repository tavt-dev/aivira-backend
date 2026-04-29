package com.tien.aivirabackend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.tien.aivirabackend.domain.dto.request.AuthenticationRequest;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.AccountErrorCode;
import com.tien.aivirabackend.exception.errorCode.PasswordErrorCode;
import com.tien.aivirabackend.repository.RoleRepository;
import com.tien.aivirabackend.repository.UserRepository;
import com.tien.aivirabackend.service.EmailService;
import com.tien.aivirabackend.service.JwtService;
import com.tien.aivirabackend.service.UserOtpService;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    UserRepository userRepository;

    @Mock
    RoleRepository roleRepository;

    @Mock
    com.tien.aivirabackend.domain.mapper.UserMapper userMapper;

    @Mock
    JwtService jwtService;

    @Mock
    UserOtpService userOtpService;

    @Mock
    EmailService emailService;

    @InjectMocks
    AuthenticationServiceImpl authenticationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authenticationService, "maxFailedLoginAttempts", 2);
        ReflectionTestUtils.setField(authenticationService, "failedLoginWindowMinutes", 15);
        ReflectionTestUtils.setField(authenticationService, "lockMinutes", 15);
        ReflectionTestUtils.setField(authenticationService, "accessTokenExpiresIn", 3600L);
    }

    @Test
    void authenticate_shouldRejectUnverifiedAccountBeforePasswordCheck() {
        User user = buildUser();
        user.setEmailVerified(false);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        AuthenticationRequest request = AuthenticationRequest.builder()
                .username("alice")
                .password("secret")
                .build();

        assertThatThrownBy(() -> authenticationService.authenticate(request, "ua", "127.0.0.1"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(AccountErrorCode.ACCOUNT_NOT_VERIFIED));

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void authenticate_shouldLockUserAfterConfiguredFailedAttempts() {
        User user = buildUser();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed-password")).thenReturn(false);

        AuthenticationRequest request = AuthenticationRequest.builder()
                .username("alice")
                .password("wrong")
                .build();

        assertThatThrownBy(() -> authenticationService.authenticate(request, "ua", "127.0.0.1"))
                .isInstanceOf(AppException.class)
                .satisfies(ex ->
                        assertThat(((AppException) ex).getErrorCode()).isEqualTo(PasswordErrorCode.PASSWORD_INCORRECT));

        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(user.getLockoutUntil()).isNull();

        assertThatThrownBy(() -> authenticationService.authenticate(request, "ua", "127.0.0.1"))
                .isInstanceOf(AppException.class)
                .satisfies(ex ->
                        assertThat(((AppException) ex).getErrorCode()).isEqualTo(PasswordErrorCode.PASSWORD_INCORRECT));

        assertThat(user.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(user.getLockoutUntil()).isNotNull();

        assertThatThrownBy(() -> authenticationService.authenticate(request, "ua", "127.0.0.1"))
                .isInstanceOf(AppException.class)
                .satisfies(ex ->
                        assertThat(((AppException) ex).getErrorCode()).isEqualTo(AccountErrorCode.ACCOUNT_LOCKED));
    }

    private User buildUser() {
        User user = new User();
        user.setId("user-1");
        user.setUsername("alice");
        user.setPassword("hashed-password");
        user.setIsDeleted(false);
        user.setIsLocked(false);
        user.setIsActive(true);
        user.setEmailVerified(true);
        user.setFailedLoginAttempts(0);
        return user;
    }
}
