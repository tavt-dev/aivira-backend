package com.tien.aivirabackend.service;

import java.util.Optional;

import org.springframework.security.oauth2.jwt.Jwt;

import com.tien.aivirabackend.domain.entity.user.User;

public interface CurrentUserService {
    User getCurrentUser();

    String getCurrentUserId();

    Jwt getCurrentJwt();

    Optional<String> findCurrentUserId();
}
