package com.tien.aivirabackend.service.impl;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.tien.aivirabackend.constant.TokenType;
import com.tien.aivirabackend.domain.entity.user.Role;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.AuthErrorCode;
import com.tien.aivirabackend.exception.errorCode.UserErrorCode;
import com.tien.aivirabackend.repository.UserRepository;
import com.tien.aivirabackend.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "JWT-SERVICE")
public class JwtServiceImpl implements JwtService {

    private static final String SCOPE_CLAIM = "scope";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String USER_ID_CLAIM = "user_id";
    //private static final String ROLES_CLAIM = "roles";

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

    }

    @Override
    public void revokeAllTokensOfUser(Long userId) {

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
        try{
            SignedJWT signedJWT = SignedJWT.parse(token);

            JWSVerifier verifier = new MACVerifier(getSignerKeyBytes());

            if (!signedJWT.verify(verifier)) {
                log.warn("Token signature verification failed");
                throw new AppException(AuthErrorCode.TOKEN_INVALID);
            }

            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

            Date expirationTime = claimsSet.getExpirationTime();
            if (expirationTime == null || new Date().after(expirationTime)) {
                log.warn("Token has expired");
                throw new AppException(AuthErrorCode.TOKEN_EXPIRED);
            }

            if(!issuer.equals(claimsSet.getIssuer())){
                log.warn("Invalid token issuer: {}", claimsSet.getIssuer());
                throw new AppException(AuthErrorCode.TOKEN_EXPIRED);
            }

            String tokenTypeInToken = claimsSet.getStringClaim(TOKEN_TYPE_CLAIM);

            if (tokenType == null || !tokenTypeInToken.equals(tokenType.name())) {
                log.error("JWT token type mismatch. Expected: {}, Found: {}", tokenType, tokenTypeInToken);
                throw new AppException(AuthErrorCode.TOKEN_INVALID);
            }

            log.debug("Token verified successfully for type: {}", tokenType);

            return signedJWT;

        }
        catch (JOSEException e) {
            log.error("Failed to verify JWT token", e);
            throw new AppException(AuthErrorCode.TOKEN_INVALID);
        }
        catch (Exception e) {
            log.error("Failed to parse JWT token", e);
            throw new AppException(AuthErrorCode.TOKEN_INVALID);
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
                .claim(SCOPE_CLAIM, buildRoles(user.getRoles()))
                .build();
    }

    List<String> buildRoles(Set<Role> roles) {
        if (CollectionUtils.isEmpty(roles)) {
            return List.of();
        }

        return roles.stream()
                .filter(Objects::nonNull)
                .map(Role::getCode)              // "USER", "ADMIN"
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
