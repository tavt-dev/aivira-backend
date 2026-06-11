package com.tien.aivirabackend.service.auth;

import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.tien.aivirabackend.config.properties.GoogleOAuthProperties;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.GoogleOAuthErrorCode;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GoogleIdTokenVerifierImpl implements GoogleIdTokenVerifier {
    final GoogleOAuthProperties properties;
    JwtDecoder jwtDecoder;

    @Override
    public GoogleUserInfo verify(String idToken, String expectedAudience) {
        try {
            Jwt jwt = decoder().decode(idToken);
            List<String> audience = jwt.getAudience();
            if (!audience.contains(expectedAudience)) {
                throw new AppException(GoogleOAuthErrorCode.GOOGLE_OAUTH_ID_TOKEN_INVALID);
            }
            String subject = jwt.getSubject();
            String email = jwt.getClaimAsString("email");
            Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");
            if (!StringUtils.hasText(subject) || !StringUtils.hasText(email)) {
                throw new AppException(GoogleOAuthErrorCode.GOOGLE_OAUTH_ID_TOKEN_INVALID);
            }
            return new GoogleUserInfo(
                    subject,
                    email,
                    Boolean.TRUE.equals(emailVerified),
                    jwt.getClaimAsString("given_name"),
                    jwt.getClaimAsString("family_name"),
                    jwt.getClaimAsString("picture"));
        } catch (AppException e) {
            throw e;
        } catch (JwtException e) {
            throw new AppException(GoogleOAuthErrorCode.GOOGLE_OAUTH_ID_TOKEN_INVALID, e);
        }
    }

    private JwtDecoder decoder() {
        if (jwtDecoder == null) {
            jwtDecoder = NimbusJwtDecoder.withIssuerLocation(properties.getIssuerUri())
                    .build();
        }
        return jwtDecoder;
    }
}
