package com.tien.aivirabackend.service;

import org.springframework.data.domain.Pageable;

import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.UserUpdateRequest;
import com.tien.aivirabackend.domain.dto.response.UserResponse;

public interface UserService {
    UserResponse getCurrentUser();

    UserResponse updateCurrentUser(UserUpdateRequest request);

    PageResponse<UserResponse> getAllUsers(Pageable pageable);

    UserResponse getUserById(String userId);
}
