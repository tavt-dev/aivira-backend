package com.tien.aivirabackend.service.impl;

import com.tien.aivirabackend.domain.dto.request.UpdatePasswordRequest;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.mapper.UserMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.AuthErrorCode;
import com.tien.aivirabackend.exception.errorCode.UserErrorCode;
import com.tien.aivirabackend.repository.UserRepository;
import com.tien.aivirabackend.service.JwtService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.tien.aivirabackend.domain.dto.request.UserUpdateRequest;
import com.tien.aivirabackend.domain.dto.response.UserResponse;
import com.tien.aivirabackend.service.UserService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j(topic = "USER-SERVICE")
public class UserServiceImpl implements UserService {

    UserRepository userRepository;

    UserMapper userMapper;

    PasswordEncoder passwordEncoder;

    JwtService jwtService;

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
    public UserResponse updateMyAvatar(MultipartFile avatarFile) {
        //Todo : validate file, save file to storage, update user avatarUrl
        return null;
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

        Integer currentVersion = currentUser.getTokenVersion() == null ? 0 : currentUser.getTokenVersion();

        currentUser.setTokenVersion(currentVersion + 1);

        userRepository.save(currentUser);

        log.info("User {} changed password", currentUser.getId());
    }

    @Override
    public void requestDeactivateMyAccount() {
        User currentUser = getCurrentUser();

        currentUser.setIsActive(false);
        currentUser.setIsDeleted(true);

        Integer currentVersion = currentUser.getTokenVersion() == null ? 0 : currentUser.getTokenVersion();
        currentUser.setTokenVersion(currentVersion + 1);

        userRepository.save(currentUser);

        jwtService.revokeAllTokensOfUser(currentUser.getId());

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

        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND));
    }
}
