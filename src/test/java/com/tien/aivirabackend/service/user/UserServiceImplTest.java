package com.tien.aivirabackend.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.tien.aivirabackend.config.properties.CloudinaryProperties;
import com.tien.aivirabackend.constant.MediaType;
import com.tien.aivirabackend.constant.PredefinedRole;
import com.tien.aivirabackend.constant.RevocationReason;
import com.tien.aivirabackend.domain.dto.request.UpdateUserRolesRequest;
import com.tien.aivirabackend.domain.dto.response.AdminUserResponse;
import com.tien.aivirabackend.domain.dto.response.UserResponse;
import com.tien.aivirabackend.domain.entity.user.Role;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.mapper.UserMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.CommonErrorCode;
import com.tien.aivirabackend.exception.errorCode.FileValidationErrorCode;
import com.tien.aivirabackend.exception.errorCode.UserErrorCode;
import com.tien.aivirabackend.repository.RoleRepository;
import com.tien.aivirabackend.repository.UserRepository;
import com.tien.aivirabackend.service.auth.CurrentUserService;
import com.tien.aivirabackend.service.auth.JwtService;
import com.tien.aivirabackend.service.media.CloudinaryStorageService;
import com.tien.aivirabackend.service.media.CloudinaryUploadResult;
import com.tien.aivirabackend.service.media.FileValidatorService;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    UserRepository userRepository;

    @Mock
    RoleRepository roleRepository;

    @Mock
    UserMapper userMapper;

    @Mock
    org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Mock
    JwtService jwtService;

    @Mock
    FileValidatorService fileValidatorService;

    @Mock
    CloudinaryStorageService cloudinaryStorageService;

    @Mock
    CloudinaryProperties cloudinaryProperties;

    @Mock
    CurrentUserService currentUserService;

    @Mock
    UserSpecifications userSpecifications;

    @InjectMocks
    UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(3600), Map.of("alg", "none"),
                Map.of("user_id", "user-1", "sub", "alice"));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        org.mockito.Mockito.lenient().when(currentUserService.getCurrentUser()).thenReturn(buildUser());
        org.mockito.Mockito.lenient().when(currentUserService.findCurrentUserId()).thenReturn(Optional.of("admin-1"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateMyAvatar_shouldValidateUploadAndSaveAvatarFields() {
        MockMultipartFile file = new MockMultipartFile("avatar", "avatar.png", "image/png",
                new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47 });
        User user = buildUser();
        UserResponse response = UserResponse.builder().id("user-1")
                .avatarUrl("https://res.cloudinary.com/demo/avatar.png").build();

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(cloudinaryProperties.getAvatarFolder()).thenReturn("aivira/users");
        when(cloudinaryStorageService.uploadImage(eq(file), eq("aivira/users/user-1/avatar"), eq("avatar-user-1"),
                eq(400), eq(400)))
                        .thenReturn(new CloudinaryUploadResult("https://res.cloudinary.com/demo/avatar.png",
                                "aivira/users/user-1/avatar/avatar-user-1"));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toUserResponse(any(User.class))).thenReturn(response);

        UserResponse result = userService.updateMyAvatar(file);

        assertThat(result).isSameAs(response);
        verify(fileValidatorService).validateFile(file, MediaType.IMAGE);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getAvatarUrl()).isEqualTo("https://res.cloudinary.com/demo/avatar.png");
        assertThat(userCaptor.getValue().getAvatarPublicId()).isEqualTo("aivira/users/user-1/avatar/avatar-user-1");
    }

    @Test
    void updateMyAvatar_shouldNotUploadOrSaveWhenValidationFails() {
        MockMultipartFile file = new MockMultipartFile("avatar", "avatar.txt", "text/plain", "hello".getBytes());
        User user = buildUser();

        when(currentUserService.getCurrentUser()).thenReturn(user);
        org.mockito.Mockito.doThrow(new AppException(FileValidationErrorCode.INVALID_MIME_TYPE))
                .when(fileValidatorService).validateFile(file, MediaType.IMAGE);

        assertThatThrownBy(() -> userService.updateMyAvatar(file)).isInstanceOf(AppException.class);

        verify(cloudinaryStorageService, never()).uploadImage(any(), any(), any(), anyInt(), anyInt());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateMyAvatar_shouldNotSaveWhenCloudinaryUploadFails() {
        MockMultipartFile file = new MockMultipartFile("avatar", "avatar.png", "image/png",
                new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47 });
        User user = buildUser();

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(cloudinaryProperties.getAvatarFolder()).thenReturn("aivira/users");
        when(cloudinaryStorageService.uploadImage(any(), any(), any(), eq(400), eq(400))).thenThrow(
                new AppException(com.tien.aivirabackend.exception.errorCode.UserErrorCode.AVATAR_UPLOAD_FAILED));

        assertThatThrownBy(() -> userService.updateMyAvatar(file)).isInstanceOf(AppException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getAdminUsers_shouldApplyFiltersAndMapResponses() {
        @SuppressWarnings("unchecked")
        Specification<User> specification = org.mockito.Mockito.mock(Specification.class);
        User user = buildUser("user-1");
        AdminUserResponse response = AdminUserResponse.builder().id("user-1").build();
        when(userSpecifications.adminUsers("alice", PredefinedRole.USER, true, false, true)).thenReturn(specification);
        when(userRepository.findAll(eq(specification), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(user)));
        when(userMapper.toAdminUserResponse(user)).thenReturn(response);

        var result = userService.getAdminUsers("alice", PredefinedRole.USER, true, false, true, 2, 10);

        assertThat(result.getData()).containsExactly(response);
        verify(userRepository).findAll(eq(specification),
                org.mockito.ArgumentMatchers.<Pageable> argThat(pageable -> pageable.getPageNumber() == 1
                        && pageable.getPageSize() == 10 && pageable.getSort().getOrderFor("createdAt") != null));
    }

    @Test
    void getAdminUser_whenUserExists_shouldReturnMappedResponse() {
        User user = buildUser("user-1");
        AdminUserResponse response = AdminUserResponse.builder().id("user-1").build();
        when(userRepository.findWithRolesById("user-1")).thenReturn(Optional.of(user));
        when(userMapper.toAdminUserResponse(user)).thenReturn(response);

        assertThat(userService.getAdminUser("user-1")).isSameAs(response);
    }

    @Test
    void getAdminUser_whenMissing_shouldThrowUserNotFoundById() {
        when(userRepository.findWithRolesById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getAdminUser("missing")).isInstanceOfSatisfying(AppException.class,
                ex -> assertThat(ex.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND_BY_ID));
    }

    @Test
    void lockUser_shouldLockClearStateRevokeTokensAndMapResponse() {
        User user = buildUser("user-1");
        user.setIsLocked(false);
        user.setLockoutUntil(Instant.now().plusSeconds(300));
        user.setFailedLoginAttempts(3);
        user.setFirstFailedLoginAt(Instant.now());
        AdminUserResponse response = AdminUserResponse.builder().id("user-1").isLocked(true).build();
        when(userRepository.findWithRolesById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toAdminUserResponse(user)).thenReturn(response);

        var result = userService.lockUser("user-1");

        assertThat(result).isSameAs(response);
        assertThat(user.getIsLocked()).isTrue();
        assertThat(user.getLockoutUntil()).isNull();
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getFirstFailedLoginAt()).isNull();
        verify(jwtService).revokeAllTokensOfUser("user-1", RevocationReason.USER_LOGOUT_ALL);
    }

    @Test
    void lockUser_whenAlreadyLocked_shouldReturnCurrentStateWithoutRevokingAgain() {
        User user = buildUser("user-1");
        user.setIsLocked(true);
        when(userRepository.findWithRolesById("user-1")).thenReturn(Optional.of(user));
        when(userMapper.toAdminUserResponse(user))
                .thenReturn(AdminUserResponse.builder().id("user-1").isLocked(true).build());

        userService.lockUser("user-1");

        verify(jwtService, never()).revokeAllTokensOfUser(eq("user-1"), any());
    }

    @Test
    void lockUser_whenSelf_shouldThrowAccessDenied() {
        assertThatThrownBy(() -> userService.lockUser("admin-1")).isInstanceOfSatisfying(AppException.class,
                ex -> assertThat(ex.getErrorCode()).isEqualTo(CommonErrorCode.ACCESS_DENIED));
    }

    @Test
    void unlockUser_shouldUnlockAndClearLockoutState() {
        User user = buildUser("user-1");
        user.setIsLocked(true);
        user.setLockoutUntil(Instant.now().plusSeconds(300));
        user.setFailedLoginAttempts(4);
        user.setFirstFailedLoginAt(Instant.now());
        when(userRepository.findWithRolesById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toAdminUserResponse(user))
                .thenReturn(AdminUserResponse.builder().id("user-1").isLocked(false).build());

        userService.unlockUser("user-1");

        assertThat(user.getIsLocked()).isFalse();
        assertThat(user.getLockoutUntil()).isNull();
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getFirstFailedLoginAt()).isNull();
        verify(jwtService, never()).revokeAllTokensOfUser(eq("user-1"), any());
    }

    @Test
    void updateUserRoles_shouldReplaceRolesWithUserRole() {
        User user = buildUser("user-1");
        user.setRoles(new java.util.HashSet<>(Set.of(role(PredefinedRole.ADMIN), role(PredefinedRole.USER))));
        Role userRole = role(PredefinedRole.USER);
        when(userRepository.findWithRolesById("user-1")).thenReturn(Optional.of(user));
        when(roleRepository.findByCode(PredefinedRole.USER)).thenReturn(Optional.of(userRole));
        when(userRepository.countActiveUsersByRole(PredefinedRole.ADMIN)).thenReturn(2L);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toAdminUserResponse(user)).thenReturn(AdminUserResponse.builder().id("user-1").build());

        userService.updateUserRoles("user-1",
                UpdateUserRolesRequest.builder().roles(Set.of(PredefinedRole.USER)).build());

        assertThat(user.getRoles()).containsExactly(userRole);
        verify(jwtService).revokeAllTokensOfUser("user-1", RevocationReason.USER_LOGOUT_ALL);
    }

    @Test
    void updateUserRoles_shouldReplaceRolesWithUserAndAdminRoles() {
        User user = buildUser("user-1");
        user.setRoles(new java.util.HashSet<>(Set.of(role(PredefinedRole.USER))));
        Role userRole = role(PredefinedRole.USER);
        Role adminRole = role(PredefinedRole.ADMIN);
        when(userRepository.findWithRolesById("user-1")).thenReturn(Optional.of(user));
        when(roleRepository.findByCode(PredefinedRole.ADMIN)).thenReturn(Optional.of(adminRole));
        when(roleRepository.findByCode(PredefinedRole.USER)).thenReturn(Optional.of(userRole));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toAdminUserResponse(user)).thenReturn(AdminUserResponse.builder().id("user-1").build());

        userService.updateUserRoles("user-1",
                UpdateUserRolesRequest.builder().roles(EnumSet.of(PredefinedRole.USER, PredefinedRole.ADMIN)).build());

        assertThat(user.getRoles()).containsExactly(userRole, adminRole);
        verify(jwtService).revokeAllTokensOfUser("user-1", RevocationReason.USER_LOGOUT_ALL);
    }

    @Test
    void updateUserRoles_whenSelf_shouldThrowAccessDenied() {
        assertThatThrownBy(() -> userService.updateUserRoles("admin-1",
                UpdateUserRolesRequest.builder().roles(Set.of(PredefinedRole.USER)).build())).isInstanceOfSatisfying(
                        AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(CommonErrorCode.ACCESS_DENIED));
    }

    @Test
    void updateUserRoles_whenRemovingLastActiveAdmin_shouldThrowCannotRemoveRole() {
        User user = buildUser("user-1");
        user.setRoles(new java.util.HashSet<>(Set.of(role(PredefinedRole.ADMIN))));
        when(userRepository.findWithRolesById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.countActiveUsersByRole(PredefinedRole.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> userService.updateUserRoles("user-1",
                UpdateUserRolesRequest.builder().roles(Set.of(PredefinedRole.USER)).build())).isInstanceOfSatisfying(
                        AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(UserErrorCode.CANNOT_REMOVE_ROLE));
    }

    @Test
    void adminMutations_whenDeletedUser_shouldReject() {
        for (String action : List.of("lock", "unlock", "roles")) {
            reset(userRepository, userMapper, jwtService, roleRepository);
            User user = buildUser("user-1");
            user.setIsDeleted(true);
            when(userRepository.findWithRolesById("user-1")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> {
                if ("lock".equals(action)) {
                    userService.lockUser("user-1");
                } else if ("unlock".equals(action)) {
                    userService.unlockUser("user-1");
                } else {
                    userService.updateUserRoles("user-1",
                            UpdateUserRolesRequest.builder().roles(Set.of(PredefinedRole.USER)).build());
                }
            }).as(action).isInstanceOfSatisfying(AppException.class,
                    ex -> assertThat(ex.getErrorCode()).isEqualTo(UserErrorCode.USER_ACCOUNT_DELETED));
        }
    }

    private User buildUser() {
        return buildUser("user-1");
    }

    private User buildUser(String id) {
        User user = new User();
        user.setId(id);
        user.setUsername(id);
        user.setEmail(id + "@example.com");
        user.setIsActive(true);
        user.setIsLocked(false);
        user.setIsDeleted(false);
        user.setEmailVerified(true);
        user.setTokenVersion(0);
        user.setFailedLoginAttempts(0);
        return user;
    }

    private Role role(PredefinedRole code) {
        return Role.builder().id((long) code.ordinal()).code(code).description(code.name()).build();
    }
}
