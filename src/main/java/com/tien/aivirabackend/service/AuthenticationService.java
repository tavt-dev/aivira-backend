package com.tien.aivirabackend.service;

import com.tien.aivirabackend.domain.dto.request.AuthenticationRequest;
import com.tien.aivirabackend.domain.dto.request.UserRegisterRequest;
import com.tien.aivirabackend.domain.dto.response.AuthenticationResponse;
import com.tien.aivirabackend.domain.dto.response.UserResponse;

public interface AuthenticationService {
    AuthenticationResponse authenticate(AuthenticationRequest request, String deviceInfo, String ipAddress);

    UserResponse register(UserRegisterRequest request);

    AuthenticationResponse refreshToken(String refreshToken, String deviceInfo, String ipAddress);

    void logout(String refreshToken);

    void logoutAllSessions();
}
