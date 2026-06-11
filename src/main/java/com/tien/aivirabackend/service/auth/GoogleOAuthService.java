package com.tien.aivirabackend.service.auth;

import com.tien.aivirabackend.domain.dto.response.AuthenticationResponse;
import com.tien.aivirabackend.domain.dto.response.GoogleOAuthAuthorizationResponse;
import com.tien.aivirabackend.domain.dto.response.GoogleOAuthCallbackResponse;

public interface GoogleOAuthService {
    GoogleOAuthAuthorizationResponse createAuthorization(String nextPath, String deviceInfo, String ipAddress);

    GoogleOAuthCallbackResponse handleCallback(String code, String state, String deviceInfo, String ipAddress);

    AuthenticationResponse exchangeTicket(String ticket, String deviceInfo, String ipAddress);

    String failureRedirectUrl(String errorCode);
}
