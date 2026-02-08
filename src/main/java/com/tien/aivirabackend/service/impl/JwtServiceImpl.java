package com.tien.aivirabackend.service.impl;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.tien.aivirabackend.constant.TokenType;
import com.tien.aivirabackend.domain.entity.InvalidatedToken;
import com.tien.aivirabackend.domain.entity.user.Role;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.AuthErrorCode;
import com.tien.aivirabackend.exception.errorCode.JwtErrorCode;
import com.tien.aivirabackend.exception.errorCode.UserErrorCode;
import com.tien.aivirabackend.repository.InvalidatedTokenRepository;
import com.tien.aivirabackend.repository.UserRepository;
import com.tien.aivirabackend.service.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "JWT-SERVICE")
public class JwtServiceImpl implements JwtService {

    private static final String SCOPE_CLAIM = "scope";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String USER_ID_CLAIM = "user_id";
    private static final String TOKEN_VERSION_CLAIM = "token_version";

    // private static final String ROLES_CLAIM = "roles";

    @NonFinal
    @Value("${jwt.signerKey}")
    private String signerKey;

    @NonFinal
    @Value("${jwt.valid-duration}")
    private long validDuration;

    @NonFinal
    @Value("${jwt.issuer}")
    private String issuer;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    private long refreshableDuration;

    private final UserRepository userRepository;

    private final InvalidatedTokenRepository invalidatedTokenRepository;

    @Override
    public String createAccessToken(User user) {
        validateUser(user);
        log.debug("Creating access token for user: {}", user.getUsername());
        return createToken(user, TokenType.ACCESS, validDuration);
    }

    @Override
    public String createRefreshToken(User user) {
        validateUser(user);
        log.debug("Creating refresh token for user: {}", user.getUsername());
        return createToken(user, TokenType.REFRESH, refreshableDuration);
    }

    @Override
    public SignedJWT verifyAccessToken(String token) {
        return verifyToken(token, TokenType.ACCESS);
    }

    @Override
    public SignedJWT verifyRefreshToken(String token) {
        return verifyToken(token, TokenType.REFRESH);
    }

    @Override
    public void revokeRefreshToken(String refreshToken) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(refreshToken);
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

            String jti = claimsSet.getJWTID();
            Date expirationTime = claimsSet.getExpirationTime();

            if (jti == null && expirationTime == null) {
                log.warn("Token missing jti or expiration time");
                throw new AppException(JwtErrorCode.TOKEN_INVALID);
            }

            // check if token is already revoked
            if (invalidatedTokenRepository.existsById(jti)) {
                log.debug("Token already revoked: {}", jti);
                throw new AppException(JwtErrorCode.TOKEN_REVOKED);
            }

            InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                    .id(jti)
                    .expiryTime(expirationTime.toInstant())
                    .build();

            invalidatedTokenRepository.save(invalidatedToken);
        } catch (ParseException e) {
            log.error("failed to parse JWT token for revocation", e);
            throw new AppException(JwtErrorCode.TOKEN_MALFORMED);
        }
    }

    @Override
    public void revokeAllTokensOfUser(String userId) {

        User user =
                userRepository.findById(userId).orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND_BY_ID));

        Integer tokenVersion = user.getTokenVersion();
        user.setTokenVersion(tokenVersion == null ? 1 : tokenVersion + 1);
        userRepository.save(user);

        log.info("Incremented token version for user {} to {}", user.getUsername(), user.getTokenVersion());
    }

    private String createToken(User user, TokenType tokenType, long validDuration) {

        try {
            JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS256);

            JWTClaimsSet jwtClaimsSet = buildJwtClaimsSet(user, tokenType, validDuration);

            SignedJWT signedJWT = new SignedJWT(jwsHeader, jwtClaimsSet);

            signedJWT.sign(new MACSigner(getSignerKeyBytes()));

            String token = signedJWT.serialize();

            log.debug("Successfully created {} token for user: {}", tokenType, user.getUsername());

            return token;
        } catch (JOSEException e) {
            log.error("Failed to create JWT token for user: {}", user.getUsername(), e);

            throw new AppException(AuthErrorCode.AUTHENTICATION_FAILED);
        }
    }

    private SignedJWT verifyToken(String token, TokenType tokenType) {
        if (token == null || token.isBlank()) {
            log.warn("Token is null or blank");
            throw new AppException(JwtErrorCode.TOKEN_MISSING);
        }

        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            JWSVerifier verifier = new MACVerifier(getSignerKeyBytes());
            if (!signedJWT.verify(verifier)) {
                log.warn("Token signature verification failed");
                throw new AppException(JwtErrorCode.TOKEN_INVALID);
            }

            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

            Date expirationTime = claimsSet.getExpirationTime();
            if (expirationTime == null || expirationTime.before(new Date())) {
                log.warn("Token has expired");
                throw new AppException(JwtErrorCode.TOKEN_EXPIRED);
            }

            String tokenIssuer = claimsSet.getIssuer();
            if (tokenIssuer == null || !issuer.equals(tokenIssuer)) {
                log.warn("Invalid token issuer. Expected: {}, Found: {}", issuer, tokenIssuer);
                throw new AppException(JwtErrorCode.TOKEN_INVALID);
            }

            if (tokenType == null) {
                log.error("Expected tokenType is null (programming error)");
                throw new AppException(JwtErrorCode.TOKEN_INVALID);
            }

            String tokenTypeInToken = claimsSet.getStringClaim(TOKEN_TYPE_CLAIM);
            if (tokenTypeInToken == null || tokenTypeInToken.isBlank()) {
                log.warn("Token missing '{}' claim", TOKEN_TYPE_CLAIM);
                throw new AppException(JwtErrorCode.TOKEN_MALFORMED);
            }

            if (!tokenType.name().equals(tokenTypeInToken)) {
                log.warn("JWT token type mismatch. Expected: {}, Found: {}", tokenType.name(), tokenTypeInToken);
                throw new AppException(JwtErrorCode.TOKEN_TYPE_INVALID);
            }

            log.debug("Token verified successfully for type: {}", tokenType.name());
            return signedJWT;

        } catch (AppException e) {
            throw e;

        } catch (JOSEException e) {
            log.error("JOSE error during JWT verification", e);
            throw new AppException(JwtErrorCode.TOKEN_INVALID);

        } catch (java.text.ParseException e) {
            log.warn("JWT parse failed (malformed token)", e);
            throw new AppException(JwtErrorCode.TOKEN_MALFORMED);

        } catch (Exception e) {
            log.error("Unexpected error during JWT verification", e);
            throw new AppException(JwtErrorCode.TOKEN_INVALID);
        }
    }

    JWTClaimsSet buildJwtClaimsSet(User user, TokenType tokenType, long validDuration) {
        Instant now = Instant.now();
        Date issueTime = Date.from(now);
        Date expirationTime = Date.from(now.plus(validDuration, ChronoUnit.SECONDS));

        return new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer(issuer)
                .issueTime(issueTime)
                .expirationTime(expirationTime)
                .jwtID(UUID.randomUUID().toString())
                .claim(TOKEN_TYPE_CLAIM, tokenType.name())
                .claim(USER_ID_CLAIM, user.getId())
                .claim(TOKEN_VERSION_CLAIM, user.getTokenVersion() != null ? user.getTokenVersion() : 0)
                .claim(SCOPE_CLAIM, buildRoles(user.getRoles()))
                .build();
    }

    List<String> buildRoles(Set<Role> roles) {
        if (CollectionUtils.isEmpty(roles)) {
            return List.of();
        }

        return roles.stream()
                .filter(Objects::nonNull)
                .map(Role::getCode) // "USER", "ADMIN"
                .filter(Objects::nonNull)
                .map(Enum::name)
                .map(code -> ROLE_PREFIX + code) // "ROLE_USER", "ROLE_ADMIN"
                .distinct()
                .toList();
    }

    private byte[] getSignerKeyBytes() {
        if (signerKey == null || signerKey.isBlank()) {
            throw new IllegalStateException("JWT signer key is not configured");
        }
        return signerKey.getBytes();
    }

    private void validateUser(User user) {
        if (user == null) {
            throw new AppException(UserErrorCode.USER_NOT_FOUND);
        }
        if (user.getId() == null) {
            throw new AppException(UserErrorCode.USER_NOT_FOUND_BY_ID);
        }
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new AppException(UserErrorCode.USER_NOT_FOUND_BY_USERNAME);
        }
    }
}
