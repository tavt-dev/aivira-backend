package com.tien.aivirabackend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.tien.aivirabackend.constant.PermissionCode;
import com.tien.aivirabackend.repository.UserPermissionRepository;
import com.tien.aivirabackend.repository.UserRepository;
import com.tien.aivirabackend.service.CurrentUserService;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceImplTest {
    @Mock
    UserRepository userRepository;

    @Mock
    UserPermissionRepository userPermissionRepository;

    @Mock
    CurrentUserService currentUserService;

    AuthorizationServiceImpl authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService =
                new AuthorizationServiceImpl(userRepository, userPermissionRepository, currentUserService);
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of("user_id", "user-1", "sub", "alice"));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        org.mockito.Mockito.lenient()
                .when(currentUserService.findCurrentUserId())
                .thenReturn(java.util.Optional.of("user-1"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void hasPermission_shouldReturnTrueWhenCurrentUserHasPermission() {
        when(userRepository.findRolePermissionCodesByUserId("user-1"))
                .thenReturn(Set.of(PermissionCode.PERMISSION_MANAGE));
        when(userPermissionRepository.findActivePermissionCodesByUserId(
                        org.mockito.ArgumentMatchers.eq("user-1"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Set.of());

        assertThat(authorizationService.hasPermission("PERMISSION_MANAGE")).isTrue();
    }

    @Test
    void hasPermission_shouldReturnTrueWhenCurrentUserHasDirectPermission() {
        when(userRepository.findRolePermissionCodesByUserId("user-1")).thenReturn(Set.of());
        when(userPermissionRepository.findActivePermissionCodesByUserId(
                        org.mockito.ArgumentMatchers.eq("user-1"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Set.of(PermissionCode.REPORT_EXPORT_ALL));

        assertThat(authorizationService.hasPermission("REPORT_EXPORT_ALL")).isTrue();
    }

    @Test
    void hasPermission_shouldReturnFalseWhenPermissionIsUnknown() {
        assertThat(authorizationService.hasPermission("NOT_A_PERMISSION")).isFalse();
    }

    @Test
    void hasAnyPermission_shouldReturnTrueWhenAnyPermissionMatches() {
        when(userRepository.findRolePermissionCodesByUserId("user-1")).thenReturn(Set.of(PermissionCode.ROLE_MANAGE));
        when(userPermissionRepository.findActivePermissionCodesByUserId(
                        org.mockito.ArgumentMatchers.eq("user-1"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Set.of());

        assertThat(authorizationService.hasAnyPermission("PERMISSION_MANAGE", "ROLE_MANAGE"))
                .isTrue();
    }
}
