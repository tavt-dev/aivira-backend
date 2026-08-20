package com.tien.aivirabackend.exception.errorCode;

import org.springframework.http.HttpStatus;

import com.tien.aivirabackend.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GoogleOAuthErrorCode implements ErrorCode {
    GOOGLE_OAUTH_DISABLED("AUTH-GOOGLE-001", "Google OAuth login is disabled", HttpStatus.SERVICE_UNAVAILABLE),
    GOOGLE_OAUTH_CONFIG_INVALID("AUTH-GOOGLE-002", "Google OAuth configuration is invalid",
            HttpStatus.INTERNAL_SERVER_ERROR),
    GOOGLE_OAUTH_STATE_INVALID("AUTH-GOOGLE-003", "Google OAuth state is invalid or expired", HttpStatus.BAD_REQUEST),
    GOOGLE_OAUTH_CODE_INVALID("AUTH-GOOGLE-004", "Google OAuth authorization code is invalid", HttpStatus.BAD_REQUEST),
    GOOGLE_OAUTH_ID_TOKEN_INVALID("AUTH-GOOGLE-005", "Google ID token is invalid", HttpStatus.UNAUTHORIZED),
    GOOGLE_EMAIL_NOT_VERIFIED("AUTH-GOOGLE-006", "Google email is not verified", HttpStatus.UNAUTHORIZED),
    GOOGLE_LOGIN_TICKET_INVALID("AUTH-GOOGLE-007", "Google login ticket is invalid or expired", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
