package com.tien.aivirabackend.config;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.JwtErrorCode;
import com.tien.aivirabackend.repository.InvalidatedTokenRepository;
import com.tien.aivirabackend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class CustomJwtDecoder implements JwtDecoder {

    private final InvalidatedTokenRepository invalidatedTokenRepository;
    private final UserRepository userRepository;

    @Value("${jwt.signerKey}")
    private String signerKey;

    @Value("${jwt.issuer}")
    private String issuer;

    private static final String TOKEN_VERSION_CLAIM = "token_version";
    private static final String USER_ID_CLAIM = "user_id";

    @Override
    public Jwt decode(String token) {
        if (token == null || token.isBlank()) {
            throw new AppException(JwtErrorCode.TOKEN_MALFORMED);
        }

        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            // Verify signature
            JWSVerifier verifier = new MACVerifier(signerKey.getBytes(StandardCharsets.UTF_8));
            if (!signedJWT.verify(verifier)) {
                log.warn("Token signature is invalid");
                throw new AppException(JwtErrorCode.TOKEN_INVALID);
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            // Validate issuer
            String tokenIssuer = claims.getIssuer();
            if (tokenIssuer == null || !issuer.equals(tokenIssuer)) {
                log.warn("Token issuer is invalid");
                throw new AppException(JwtErrorCode.TOKEN_INVALID);
            }

            // Validate expiration
            Date expirationTime = claims.getExpirationTime();
            if (expirationTime == null || expirationTime.toInstant().isBefore(Instant.now())) {
                log.warn("token has expired");
                throw new AppException(JwtErrorCode.TOKEN_EXPIRED);
            }

            // Check if token is invalidated(revoked)
            String jti = claims.getJWTID();
            if (jti != null && invalidatedTokenRepository.existsById(jti)) {
                log.warn("Token has been invalidated: {}", jti);
                throw new AppException(JwtErrorCode.TOKEN_REVOKED);
            }

            // Check token version
            Long tokenVersion = claims.getLongClaim(TOKEN_VERSION_CLAIM);
            String userId = claims.getStringClaim(USER_ID_CLAIM);

            if (userId != null && tokenVersion != null) {
                userRepository.findById(userId).ifPresent(user -> {
                    Integer currentTokenVersion = user.getTokenVersion();
                    if (currentTokenVersion != null && tokenVersion < currentTokenVersion.longValue()) {
                        log.warn("Token version mismatch. Token has old version: {}, current: {}", tokenVersion,
                                currentTokenVersion);
                        throw new AppException(JwtErrorCode.TOKEN_REVOKED);
                    }
                });
            }

            // Build Spring Security Jwt object
            Date iat = claims.getIssueTime();
            Instant issueAt = iat != null ? iat.toInstant() : Instant.now();

            return new Jwt(token, issueAt, expirationTime.toInstant(), signedJWT.getHeader().toJSONObject(),
                    claims.toJSONObject());
        } catch (AppException e) {
            throw e;
        } catch (ParseException e) {
            log.warn("Token parse exception: {}", e.getMessage());
            throw new AppException(JwtErrorCode.TOKEN_MALFORMED);
        } catch (JOSEException e) {
            log.error("JOSE error during token verification", e);
            throw new AppException(JwtErrorCode.TOKEN_INVALID);
        }
    }
}
