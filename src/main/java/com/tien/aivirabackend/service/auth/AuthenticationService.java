package com.tien.aivirabackend.service.auth;

import java.util.List;

import com.tien.aivirabackend.domain.dto.request.AuthenticationRequest;
import com.tien.aivirabackend.domain.dto.request.ForgotPasswordRequest;
import com.tien.aivirabackend.domain.dto.request.ResendOtpRequest;
import com.tien.aivirabackend.domain.dto.request.ResetPasswordRequest;
import com.tien.aivirabackend.domain.dto.request.UserRegisterRequest;
import com.tien.aivirabackend.domain.dto.request.VerifyUserRequest;
import com.tien.aivirabackend.domain.dto.response.ActiveSessionResponse;
import com.tien.aivirabackend.domain.dto.response.AuthenticationResponse;
import com.tien.aivirabackend.domain.dto.response.UserResponse;

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
