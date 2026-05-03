package com.tien.aivirabackend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.tien.aivirabackend.config.properties.CloudinaryProperties;
import com.tien.aivirabackend.constant.MediaType;
import com.tien.aivirabackend.domain.dto.response.UserResponse;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.mapper.UserMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.FileValidationErrorCode;
import com.tien.aivirabackend.repository.UserRepository;
import com.tien.aivirabackend.service.CloudinaryStorageService;
import com.tien.aivirabackend.service.CloudinaryUploadResult;
import com.tien.aivirabackend.service.CurrentUserService;
import com.tien.aivirabackend.service.FileValidatorService;
import com.tien.aivirabackend.service.JwtService;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    UserRepository userRepository;

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

    @InjectMocks
    UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of("user_id", "user-1", "sub", "alice"));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
        org.mockito.Mockito.lenient().when(currentUserService.getCurrentUser()).thenReturn(buildUser());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateMyAvatar_shouldValidateUploadAndSaveAvatarFields() {
        MockMultipartFile file =
                new MockMultipartFile("avatar", "avatar.png", "image/png", new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47});
        User user = buildUser();
        UserResponse response = UserResponse.builder()
                .id("user-1")
                .avatarUrl("https://res.cloudinary.com/demo/avatar.png")
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(cloudinaryProperties.getAvatarFolder()).thenReturn("aivira/users");
        when(cloudinaryStorageService.uploadImage(
                        eq(file), eq("aivira/users/user-1/avatar"), eq("avatar-user-1"), eq(400), eq(400)))
                .thenReturn(new CloudinaryUploadResult(
                        "https://res.cloudinary.com/demo/avatar.png", "aivira/users/user-1/avatar/avatar-user-1"));
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
                .when(fileValidatorService)
                .validateFile(file, MediaType.IMAGE);

        assertThatThrownBy(() -> userService.updateMyAvatar(file)).isInstanceOf(AppException.class);

        verify(cloudinaryStorageService, never()).uploadImage(any(), any(), any(), anyInt(), anyInt());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateMyAvatar_shouldNotSaveWhenCloudinaryUploadFails() {
        MockMultipartFile file =
                new MockMultipartFile("avatar", "avatar.png", "image/png", new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47});
        User user = buildUser();

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(cloudinaryProperties.getAvatarFolder()).thenReturn("aivira/users");
        when(cloudinaryStorageService.uploadImage(any(), any(), any(), eq(400), eq(400)))
                .thenThrow(new AppException(
                        com.tien.aivirabackend.exception.errorCode.UserErrorCode.AVATAR_UPLOAD_FAILED));

        assertThatThrownBy(() -> userService.updateMyAvatar(file)).isInstanceOf(AppException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    private User buildUser() {
        User user = new User();
        user.setId("user-1");
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        return user;
    }
}
