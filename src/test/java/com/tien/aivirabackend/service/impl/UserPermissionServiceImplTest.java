package com.tien.aivirabackend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.tien.aivirabackend.constant.PermissionCode;
import com.tien.aivirabackend.domain.dto.request.GrantUserPermissionRequest;
import com.tien.aivirabackend.domain.entity.user.Permission;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.entity.user.UserPermission;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.repository.PermissionRepository;
import com.tien.aivirabackend.repository.UserPermissionRepository;
import com.tien.aivirabackend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserPermissionServiceImplTest {
    @Mock
    UserRepository userRepository;

    @Mock
    PermissionRepository permissionRepository;

    @Mock
    UserPermissionRepository userPermissionRepository;

    @InjectMocks
    UserPermissionServiceImpl userPermissionService;

    @BeforeEach
    void setUp() {
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of("user_id", "admin-1", "sub", "admin"));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getUserPermissions_shouldReturnUnionOfRoleAndActiveDirectPermissions() {
        Permission rolePermission = buildPermission(PermissionCode.USER_READ_SELF);
        Permission directPermission = buildPermission(PermissionCode.REPORT_EXPORT_ALL);
        UserPermission userPermission = UserPermission.builder()
                .id(1L)
                .permission(directPermission)
                .grantedAt(Instant.now())
                .active(true)
                .build();

        when(userRepository.existsById("user-1")).thenReturn(true);
        when(userRepository.findRolePermissionCodesByUserId("user-1"))
                .thenReturn(Set.of(PermissionCode.USER_READ_SELF));
        when(userPermissionRepository.findActivePermissionCodesByUserId(
                        org.mockito.ArgumentMatchers.eq("user-1"), any()))
                .thenReturn(Set.of(PermissionCode.REPORT_EXPORT_ALL));
        when(permissionRepository.findByCodeIn(Set.of(PermissionCode.USER_READ_SELF, PermissionCode.REPORT_EXPORT_ALL)))
                .thenReturn(List.of(rolePermission, directPermission));
        when(userPermissionRepository.findAllByUserId("user-1")).thenReturn(List.of(userPermission));

        var response = userPermissionService.getUserPermissions("user-1");

        assertThat(response.getEffectivePermissions())
                .extracting("code")
                .containsExactly(PermissionCode.USER_READ_SELF, PermissionCode.REPORT_EXPORT_ALL);
        assertThat(response.getDirectPermissions()).hasSize(1);
    }

    @Test
    void grantPermission_shouldRejectSelfGrant() {
        assertThatThrownBy(() -> userPermissionService.grantPermission(
                        "admin-1",
                        GrantUserPermissionRequest.builder()
                                .permissionCode(PermissionCode.REPORT_EXPORT_ALL)
                                .build()))
                .isInstanceOf(AppException.class);
    }

    @Test
    void grantPermission_shouldSaveDirectPermission() {
        User target = buildUser("user-1");
        User admin = buildUser("admin-1");
        Permission permission = buildPermission(PermissionCode.REPORT_EXPORT_ALL);

        when(userRepository.findById("user-1")).thenReturn(Optional.of(target));
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(admin));
        when(permissionRepository.findByCode(PermissionCode.REPORT_EXPORT_ALL)).thenReturn(Optional.of(permission));
        when(userPermissionRepository.existsByUser_IdAndPermission_CodeAndActiveTrue(
                        "user-1", PermissionCode.REPORT_EXPORT_ALL))
                .thenReturn(false);
        when(userPermissionRepository.save(any(UserPermission.class))).thenAnswer(invocation -> {
            UserPermission userPermission = invocation.getArgument(0);
            userPermission.setId(1L);
            return userPermission;
        });

        userPermissionService.grantPermission(
                "user-1",
                GrantUserPermissionRequest.builder()
                        .permissionCode(PermissionCode.REPORT_EXPORT_ALL)
                        .reason("temporary report access")
                        .build());

        ArgumentCaptor<UserPermission> captor = ArgumentCaptor.forClass(UserPermission.class);
        verify(userPermissionRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(target);
        assertThat(captor.getValue().getGrantedBy()).isSameAs(admin);
        assertThat(captor.getValue().getPermission()).isSameAs(permission);
        assertThat(captor.getValue().getActive()).isTrue();
    }

    @Test
    void revokePermission_shouldMarkDirectPermissionInactive() {
        UserPermission userPermission = UserPermission.builder()
                .id(1L)
                .permission(buildPermission(PermissionCode.REPORT_EXPORT_ALL))
                .grantedAt(Instant.now())
                .active(true)
                .build();
        when(userPermissionRepository.findFirstByUser_IdAndPermission_CodeAndActiveTrueOrderByGrantedAtDesc(
                        "user-1", PermissionCode.REPORT_EXPORT_ALL))
                .thenReturn(Optional.of(userPermission));

        userPermissionService.revokePermission("user-1", PermissionCode.REPORT_EXPORT_ALL);

        assertThat(userPermission.getActive()).isFalse();
        assertThat(userPermission.getRevokedAt()).isNotNull();
        verify(userPermissionRepository).save(userPermission);
    }

    private User buildUser(String id) {
        User user = new User();
        user.setId(id);
        user.setUsername(id);
        user.setEmail(id + "@example.com");
        return user;
    }

    private Permission buildPermission(PermissionCode code) {
        return Permission.builder()
                .id((long) code.ordinal())
                .code(code)
                .name(code.name())
                .description(code.name())
                .group(code.getGroup())
                .system(true)
                .build();
    }
}
