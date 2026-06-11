package com.tien.aivirabackend.service.user;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.tien.aivirabackend.config.properties.CloudinaryProperties;
import com.tien.aivirabackend.constant.MediaType;
import com.tien.aivirabackend.constant.PredefinedRole;
import com.tien.aivirabackend.constant.RevocationReason;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.UpdatePasswordRequest;
import com.tien.aivirabackend.domain.dto.request.UpdateUserRolesRequest;
import com.tien.aivirabackend.domain.dto.request.UserUpdateRequest;
import com.tien.aivirabackend.domain.dto.response.AdminUserResponse;
import com.tien.aivirabackend.domain.dto.response.UserResponse;
import com.tien.aivirabackend.domain.entity.user.Role;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.mapper.UserMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.CommonErrorCode;
import com.tien.aivirabackend.exception.errorCode.UserErrorCode;
import com.tien.aivirabackend.repository.RoleRepository;
import com.tien.aivirabackend.repository.UserRepository;
import com.tien.aivirabackend.service.auth.CurrentUserService;
import com.tien.aivirabackend.service.auth.JwtService;
import com.tien.aivirabackend.service.media.CloudinaryStorageService;
import com.tien.aivirabackend.service.media.CloudinaryUploadResult;
import com.tien.aivirabackend.service.media.FileValidatorService;
import com.tien.aivirabackend.util.PageRequestUtils;

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

    RoleRepository roleRepository;

    UserMapper userMapper;

    PasswordEncoder passwordEncoder;

    JwtService jwtService;

    FileValidatorService fileValidatorService;

    CloudinaryStorageService cloudinaryStorageService;

    CloudinaryProperties cloudinaryProperties;

    CurrentUserService currentUserService;

    UserSpecifications userSpecifications;

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

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> getAdminUsers(
            String keyword,
            PredefinedRole role,
            Boolean active,
            Boolean locked,
            Boolean emailVerified,
            int page,
            int size) {
        Specification<User> specification = userSpecifications.adminUsers(keyword, role, active, locked, emailVerified);
        var userPage = userRepository
                .findAll(specification, PageRequestUtils.newestFirst(page, size))
                .map(userMapper::toAdminUserResponse);
        return PageResponse.from(userPage);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse getAdminUser(String userId) {
        User user = findUserWithRoles(userId);
        return userMapper.toAdminUserResponse(user);
    }

    @Override
    @Transactional
    public AdminUserResponse lockUser(String userId) {
        rejectSelfAction(userId);
        User user = findUserWithRoles(userId);
        rejectDeletedUser(user);
        clearLockoutState(user);

        if (!Boolean.TRUE.equals(user.getIsLocked())) {
            user.setIsLocked(true);
            userRepository.save(user);
            jwtService.revokeAllTokensOfUser(userId, RevocationReason.USER_LOGOUT_ALL);
            log.info("admin_user_locked targetUserId={} adminUserId={}", userId, currentAdminUserId());
        }

        return userMapper.toAdminUserResponse(user);
    }

    @Override
    @Transactional
    public AdminUserResponse unlockUser(String userId) {
        User user = findUserWithRoles(userId);
        rejectDeletedUser(user);
        clearLockoutState(user);

        if (Boolean.TRUE.equals(user.getIsLocked())) {
            user.setIsLocked(false);
            userRepository.save(user);
            log.info("admin_user_unlocked targetUserId={} adminUserId={}", userId, currentAdminUserId());
        }

        return userMapper.toAdminUserResponse(user);
    }

    @Override
    @Transactional
    public AdminUserResponse updateUserRoles(String userId, UpdateUserRolesRequest request) {
        rejectSelfAction(userId);
        if (request == null || CollectionUtils.isEmpty(request.getRoles())) {
            throw new AppException(CommonErrorCode.INVALID_INPUT);
        }

        User user = findUserWithRoles(userId);
        rejectDeletedUser(user);

        Set<PredefinedRole> requestedRoleCodes = EnumSet.copyOf(request.getRoles());
        Set<PredefinedRole> currentRoleCodes = roleCodes(user);
        validateLastActiveAdmin(user, currentRoleCodes, requestedRoleCodes);

        Set<Role> requestedRoles = requestedRoleCodes.stream()
                .sorted()
                .map(this::findRole)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (!Objects.equals(currentRoleCodes, requestedRoleCodes)) {
            user.setRoles(requestedRoles);
            userRepository.save(user);
            jwtService.revokeAllTokensOfUser(userId, RevocationReason.USER_LOGOUT_ALL);
            log.info(
                    "admin_user_roles_updated targetUserId={} adminUserId={} oldRoles={} newRoles={}",
                    userId,
                    currentAdminUserId(),
                    currentRoleCodes,
                    requestedRoleCodes);
        }

        return userMapper.toAdminUserResponse(user);
    }

    private User getCurrentUser() {
        return currentUserService.getCurrentUser();
    }

    private User findUserWithRoles(String userId) {
        return userRepository
                .findWithRolesById(userId)
                .orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND_BY_ID));
    }

    private Role findRole(PredefinedRole role) {
        return roleRepository.findByCode(role).orElseThrow(() -> new AppException(UserErrorCode.ROLE_NOT_FOUND));
    }

    private void rejectSelfAction(String targetUserId) {
        String currentUserId = currentAdminUserId();
        if (StringUtils.hasText(currentUserId) && currentUserId.equals(targetUserId)) {
            throw new AppException(CommonErrorCode.ACCESS_DENIED);
        }
    }

    private void rejectDeletedUser(User user) {
        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new AppException(UserErrorCode.USER_ACCOUNT_DELETED);
        }
    }

    private void clearLockoutState(User user) {
        user.setLockoutUntil(null);
        user.setFailedLoginAttempts(0);
        user.setFirstFailedLoginAt(null);
    }

    private void validateLastActiveAdmin(
            User user, Set<PredefinedRole> currentRoleCodes, Set<PredefinedRole> requestedRoleCodes) {
        if (!currentRoleCodes.contains(PredefinedRole.ADMIN) || requestedRoleCodes.contains(PredefinedRole.ADMIN)) {
            return;
        }
        if (!Boolean.TRUE.equals(user.getIsActive()) || Boolean.TRUE.equals(user.getIsDeleted())) {
            return;
        }
        if (userRepository.countActiveUsersByRole(PredefinedRole.ADMIN) <= 1) {
            throw new AppException(UserErrorCode.CANNOT_REMOVE_ROLE);
        }
    }

    private Set<PredefinedRole> roleCodes(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return EnumSet.noneOf(PredefinedRole.class);
        }
        return user.getRoles().stream()
                .map(Role::getCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(PredefinedRole.class)));
    }

    private String currentAdminUserId() {
        var userId = currentUserService.findCurrentUserId();
        return userId == null ? null : userId.orElse(null);
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
