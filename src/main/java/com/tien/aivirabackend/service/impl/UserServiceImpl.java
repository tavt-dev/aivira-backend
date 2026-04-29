package com.tien.aivirabackend.service.impl;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tien.aivirabackend.config.properties.CloudinaryProperties;
import com.tien.aivirabackend.constant.MediaType;
import com.tien.aivirabackend.constant.RevocationReason;
import com.tien.aivirabackend.domain.dto.request.UpdatePasswordRequest;
import com.tien.aivirabackend.domain.dto.request.UserUpdateRequest;
import com.tien.aivirabackend.domain.dto.response.UserResponse;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.mapper.UserMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.AuthErrorCode;
import com.tien.aivirabackend.exception.errorCode.UserErrorCode;
import com.tien.aivirabackend.repository.UserRepository;
import com.tien.aivirabackend.service.CloudinaryStorageService;
import com.tien.aivirabackend.service.CloudinaryUploadResult;
import com.tien.aivirabackend.service.FileValidatorService;
import com.tien.aivirabackend.service.JwtService;
import com.tien.aivirabackend.service.UserService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j(topic = "USER-SERVICE")
public class UserServiceImpl implements UserService {
    private static final int AVATAR_WIDTH = 400;
    private static final int AVATAR_HEIGHT = 400;

    UserRepository userRepository;

    UserMapper userMapper;

    PasswordEncoder passwordEncoder;

    JwtService jwtService;

    FileValidatorService fileValidatorService;

    CloudinaryStorageService cloudinaryStorageService;

    CloudinaryProperties cloudinaryProperties;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMyProfile() {
        User currentUser = getCurrentUser();
        return userMapper.toUserResponse(currentUser);
    }

    @Override
    public UserResponse updateMyProfile(UserUpdateRequest request) {
        User currentUser = getCurrentUser();

        if (request.getFirstName() != null) {
            currentUser.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            currentUser.setLastName(request.getLastName());
        }
        if (request.getGender() != null) {
            currentUser.setGender(request.getGender());
        }
        User savedUser = userRepository.save(currentUser);
        log.info("User {} updated profile", savedUser.getId());

        return userMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse updateMyAvatar(MultipartFile avatarFile) {
        User currentUser = getCurrentUser();

        fileValidatorService.validateFile(avatarFile, MediaType.IMAGE);

        CloudinaryUploadResult uploadResult = cloudinaryStorageService.uploadImage(
                avatarFile,
                buildAvatarFolder(currentUser.getId()),
                "avatar-" + currentUser.getId(),
                AVATAR_WIDTH,
                AVATAR_HEIGHT);

        currentUser.setAvatarUrl(uploadResult.secureUrl());
        currentUser.setAvatarPublicId(uploadResult.publicId());

        User savedUser = userRepository.save(currentUser);
        log.info("User {} updated avatar", savedUser.getId());

        return userMapper.toUserResponse(savedUser);
    }

    @Override
    public void changeMyPassword(UpdatePasswordRequest request) {
        User currentUser = getCurrentUser();

        if (currentUser.getPassword() == null || currentUser.getPassword().isBlank()) {
            throw new AppException(UserErrorCode.USER_PASSWORD_NOT_SET);
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new AppException(UserErrorCode.INVALID_CURRENT_PASSWORD);
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(UserErrorCode.PASSWORD_CONFIRMATION_DOES_NOT_MATCH);
        }

        if (passwordEncoder.matches(request.getNewPassword(), currentUser.getPassword())) {
            throw new AppException(UserErrorCode.NEW_PASSWORD_MUST_BE_DIFFERENT);
        }

        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(currentUser);
        jwtService.revokeAllTokensOfUser(currentUser.getId(), RevocationReason.PASSWORD_CHANGE);

        log.info("User {} changed password", currentUser.getId());
    }

    @Override
    public void requestDeactivateMyAccount() {
        User currentUser = getCurrentUser();

        currentUser.setIsActive(false);
        currentUser.setIsDeleted(true);

        userRepository.save(currentUser);

        jwtService.revokeAllTokensOfUser(currentUser.getId(), RevocationReason.USER_LOGOUT_ALL);

        log.info("User {} requested account deactivation", currentUser.getId());
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(AuthErrorCode.AUTHENTICATION_FAILED);
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            throw new AppException(AuthErrorCode.AUTHENTICATION_FAILED);
        }

        String userId = jwt.getClaimAsString("user_id");
        if (userId == null || userId.isBlank()) {
            throw new AppException(AuthErrorCode.AUTHENTICATION_FAILED);
        }

        return userRepository.findById(userId).orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND));
    }

    private String buildAvatarFolder(String userId) {
        String baseFolder = cloudinaryProperties.getAvatarFolder();
        if (baseFolder == null || baseFolder.isBlank()) {
            baseFolder = "aivira/users";
        }

        return trimSlashes(baseFolder) + "/" + userId + "/avatar";
    }

    private String trimSlashes(String value) {
        String trimmed = value.trim();
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
