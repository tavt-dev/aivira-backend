package com.tien.aivirabackend.service.impl;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.UserUpdateRequest;
import com.tien.aivirabackend.domain.dto.response.UserResponse;
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
    @Override
    public UserResponse getCurrentUser() {
        return null;
    }

    @Override
    public UserResponse updateCurrentUser(UserUpdateRequest request) {
        return null;
    }

    @Override
    public PageResponse<UserResponse> getAllUsers(Pageable pageable) {
        return null;
    }

    @Override
    public UserResponse getUserById(String userId) {
        return null;
    }
}
