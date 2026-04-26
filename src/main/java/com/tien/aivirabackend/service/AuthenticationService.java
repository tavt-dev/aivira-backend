package com.tien.aivirabackend.service;

import com.tien.aivirabackend.domain.dto.request.*;
import com.tien.aivirabackend.domain.dto.response.ActiveSessionResponse;
import com.tien.aivirabackend.domain.dto.response.AuthenticationResponse;
import com.tien.aivirabackend.domain.dto.response.UserResponse;

import java.util.List;

public interface AuthenticationService {
    AuthenticationResponse authenticate(AuthenticationRequest request, String deviceInfo, String ipAddress);

    UserResponse register(UserRegisterRequest request);

    AuthenticationResponse refreshToken(String refreshToken, String deviceInfo, String ipAddress);

    void logout(String refreshToken);

    void logoutAll();

    List<ActiveSessionResponse> getActiveSessions();

    void revokeSession(String sessionId);

    void verifyUser(VerifyUserRequest request);

    void resendVerificationOtp(ResendOtpRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
