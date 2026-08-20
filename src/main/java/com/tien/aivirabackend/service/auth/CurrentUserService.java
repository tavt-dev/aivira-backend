package com.tien.aivirabackend.service.auth;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.AuthErrorCode;
import com.tien.aivirabackend.exception.errorCode.UserErrorCode;
import com.tien.aivirabackend.repository.UserRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CurrentUserService {
    private static final String USER_ID_CLAIM = "user_id";

    UserRepository userRepository;

    public User getCurrentUser() {
        return userRepository.findById(getCurrentUserId())
                .orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND));
    }

    public String getCurrentUserId() {
        String userId = getCurrentJwt().getClaimAsString(USER_ID_CLAIM);
        if (!StringUtils.hasText(userId)) {
            throw new AppException(AuthErrorCode.AUTHENTICATION_FAILED);
        }
        return userId;
    }

    public Jwt getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(AuthErrorCode.AUTHENTICATION_FAILED);
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            throw new AppException(AuthErrorCode.AUTHENTICATION_FAILED);
        }

        return jwt;
    }

    public Optional<String> findCurrentUserId() {
        try {
            return Optional.of(getCurrentUserId());
        } catch (AppException ex) {
            return Optional.empty();
        }
    }
}
