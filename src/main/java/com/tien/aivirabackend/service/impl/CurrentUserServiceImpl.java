package com.tien.aivirabackend.service.impl;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.AuthErrorCode;
import com.tien.aivirabackend.exception.errorCode.UserErrorCode;
import com.tien.aivirabackend.repository.UserRepository;
import com.tien.aivirabackend.service.CurrentUserService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CurrentUserServiceImpl implements CurrentUserService {
    UserRepository userRepository;

    @Override
    public User getCurrentUser() {
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
}
