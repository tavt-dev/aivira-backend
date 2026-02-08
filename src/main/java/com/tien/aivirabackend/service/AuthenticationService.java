package com.tien.aivirabackend.service;

import com.tien.aivirabackend.domain.dto.request.AuthenticationRequest;
import com.tien.aivirabackend.domain.dto.request.UserRegisterRequest;
import com.tien.aivirabackend.domain.dto.response.AuthenticationResponse;
import com.tien.aivirabackend.domain.dto.response.UserResponse;

public interface AuthenticationService {
    AuthenticationResponse authenticate(AuthenticationRequest request);

    UserResponse register(UserRegisterRequest request);

    AuthenticationResponse refreshToken(String refreshToken);

    void logout(String refreshToken);

    void logoutAllSessions();
}
