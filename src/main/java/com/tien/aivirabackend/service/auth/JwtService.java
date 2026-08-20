package com.tien.aivirabackend.service.auth;

import static org.springframework.util.StringUtils.truncate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.tien.aivirabackend.constant.RevocationReason;
import com.tien.aivirabackend.constant.TokenType;
import com.tien.aivirabackend.domain.dto.response.ActiveSessionResponse;
import com.tien.aivirabackend.domain.entity.RefreshToken;
import com.tien.aivirabackend.domain.entity.user.Role;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.AuthErrorCode;
import com.tien.aivirabackend.exception.errorCode.JwtErrorCode;
import com.tien.aivirabackend.exception.errorCode.UserErrorCode;
import com.tien.aivirabackend.repository.RefreshTokenRepository;
import com.tien.aivirabackend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "JWT-SERVICE")
public class JwtService {

    private static final String SCOPE_CLAIM = "scope";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String USER_ID_CLAIM = "user_id";
    private static final String TOKEN_VERSION_CLAIM = "token_version";
    private static final String FAMILY_ID_CLAIM = "family_id";

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

    private final RefreshTokenRepository refreshTokenRepository;

    public String createAccessToken(User user) {
        validateUser(user);
        log.debug("Creating access token for user: {}", user.getUsername());
        return createToken(user, TokenType.ACCESS, validDuration, null);
    }

    public long getAccessTokenExpiresIn() {
        return validDuration;
    }

    @Transactional
    public String createRefreshToken(User user, String deviceInfo, String ipAddress, String familyId) {
        validateUser(user);
        log.debug("Creating refresh token for user: {}", user.getUsername());

        String tokenFamilyId = familyId != null ? familyId : UUID.randomUUID().toString();

        String token = createToken(user, TokenType.REFRESH, refreshableDuration, tokenFamilyId);

        JWTClaimsSet claimsSet = parseClaims(token, JwtErrorCode.TOKEN_MALFORMED, "store refresh token");
        RefreshToken refreshToken = buildRefreshTokenRecord(user, token, tokenFamilyId, deviceInfo, ipAddress,
                claimsSet);
        refreshTokenRepository.save(refreshToken);
        log.debug("Refresh token stored successfully for user: {}", user.getUsername());

        return token;
    }

    public SignedJWT verifyAccessToken(String token) {
        return verifyToken(token, TokenType.ACCESS);
    }

    @Transactional
    public SignedJWT verifyRefreshToken(String refreshToken) {
        SignedJWT signedJWT = verifyToken(refreshToken, TokenType.REFRESH);

        JWTClaimsSet claimsSet = getClaimsSet(signedJWT, JwtErrorCode.TOKEN_INVALID, "verify refresh token");
        RefreshToken storedToken = findStoredRefreshToken(claimsSet.getJWTID());
        validateStoredRefreshToken(storedToken);
        storedToken.markUsed(Instant.now());
        refreshTokenRepository.save(storedToken);

        return signedJWT;
    }

    @Transactional
    public void revokeRefreshToken(String refreshToken) {
        revokeRefreshToken(refreshToken, RevocationReason.TOKEN_REFRESH, null);
    }

    @Transactional
    public void revokeRefreshToken(String refreshToken, RevocationReason reason, String replacedBy) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(refreshToken);
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

            String jti = claimsSet.getJWTID();

            if (jti == null) {
                log.warn("Token missing jti or expiration time");
                throw new AppException(JwtErrorCode.TOKEN_INVALID);
            }

            Optional<RefreshToken> storedTokenOpt = refreshTokenRepository.findByJti(jti);

            if (storedTokenOpt.isPresent()) {
                revokeStoredRefreshToken(storedTokenOpt.get(), jti, reason, replacedBy);
            } else {
                log.warn("Token not found in database: {}", jti);
            }

        } catch (ParseException e) {
            log.error("failed to parse JWT token for revocation", e);
            throw new AppException(JwtErrorCode.TOKEN_INVALID);
        }
    }

    @Transactional
    public void revokeAllTokensOfUser(String userId) {
        revokeAllTokensOfUser(userId, RevocationReason.USER_LOGOUT_ALL);
    }

    @Transactional
    public void revokeAllTokensOfUser(String userId, RevocationReason reason) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND_BY_ID));

        Integer tokenVersion = user.getTokenVersion();
        user.setTokenVersion(tokenVersion == null ? 1 : tokenVersion + 1);
        userRepository.save(user);

        // Revoke all refresh tokens in database

        int revokedCount = refreshTokenRepository.revokeAllByUserId(userId, Instant.now(),
                reason == null ? RevocationReason.USER_LOGOUT_ALL : reason);
        log.info("Revoked {} refresh tokens and incremented token version for user {} to {}", revokedCount,
                user.getUsername(), user.getTokenVersion());
    }

    public String getTokenFamilyId(String refreshToken) {
        SignedJWT signedJWT = verifyToken(refreshToken, TokenType.REFRESH);
        JWTClaimsSet claimsSet = getClaimsSet(signedJWT, JwtErrorCode.TOKEN_INVALID, "read refresh token family ID");
        return getStringClaim(claimsSet, FAMILY_ID_CLAIM, "read refresh token family ID");
    }

    public String getTokenJti(String token) {
        SignedJWT signedJWT = verifyToken(token, TokenType.REFRESH);
        return getClaimsSet(signedJWT, JwtErrorCode.TOKEN_INVALID, "read refresh token JTI").getJWTID();
    }

    public void revokeSession(String userId, String sessionId) {
        RefreshToken token = refreshTokenRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(JwtErrorCode.REFRESH_TOKEN_INVALID));

        // Verify token belongs to user
        if (!token.getUser().getId().equals(userId)) {
            log.warn("User {} attempted to revoke session {} which does not belong to them", userId, sessionId);
            throw new AppException(JwtErrorCode.REFRESH_TOKEN_INVALID);
        }

        if (!token.isRevoked()) {
            token.revoke(RevocationReason.USER_LOGOUT);
            refreshTokenRepository.save(token);
            log.info("Session {} revoked successfully for user {}", sessionId, userId);
        }
    }

    @Transactional(readOnly = true)
    public List<ActiveSessionResponse> getActiveSessions(String userId, String currentSessionJti) {
        List<RefreshToken> tokens = refreshTokenRepository.findActiveTokensByUserId(userId, Instant.now());

        return tokens.stream()
                .map(token -> ActiveSessionResponse.builder().sessionId(token.getId()).deviceInfo(token.getDeviceInfo())
                        .ipAddress(token.getIpAddress()).createdAt(token.getIssuedAt()).expiresAt(token.getExpiresAt())
                        .currentSession(token.getJti().equals(currentSessionJti)).build())
                .toList();
    }

    private RefreshToken buildRefreshTokenRecord(User user, String token, String tokenFamilyId, String deviceInfo,
            String ipAddress, JWTClaimsSet claimsSet) {
        return RefreshToken.builder().tokenHash(hashToken(token)).jti(claimsSet.getJWTID()).familyId(tokenFamilyId)
                .user(user).issuedAt(claimsSet.getIssueTime().toInstant())
                .expiresAt(claimsSet.getExpirationTime().toInstant()).deviceInfo(truncate(deviceInfo, 512))
                .ipAddress(truncate(ipAddress, 45)).revoked(false).build();
    }

    private RefreshToken findStoredRefreshToken(String jti) {
        return refreshTokenRepository.findByJti(jti).orElseThrow(() -> {
            log.warn("Refresh token not found in database: {}", jti);
            return new AppException(JwtErrorCode.REFRESH_TOKEN_INVALID);
        });
    }

    private void validateStoredRefreshToken(RefreshToken storedToken) {
        if (storedToken.isRevoked()) {
            revokeTokenFamilyAfterReuse(storedToken);
            throw new AppException(JwtErrorCode.REFRESH_TOKEN_REUSED);
        }

        if (!storedToken.isValid()) {
            log.warn("Refresh token is no longer valid (expired or revoked): {}", storedToken.getJti());
            throw new AppException(JwtErrorCode.REFRESH_TOKEN_EXPIRED);
        }
    }

    private void revokeTokenFamilyAfterReuse(RefreshToken storedToken) {
        String familyId = storedToken.getFamilyId();
        log.warn("SECURITY: Refresh token reuse detected! JTI: {}, Family: {}", storedToken.getJti(), familyId);

        int revokedCount = refreshTokenRepository.revokeAllByFamilyId(familyId, Instant.now(),
                RevocationReason.SECURITY_BREACH);
        log.warn("Revoked {} tokens in family {} due to reuse detection", revokedCount, familyId);
    }

    private void revokeStoredRefreshToken(RefreshToken storedToken, String jti, RevocationReason reason,
            String replacedBy) {
        if (storedToken.isRevoked()) {
            log.debug("Token already revoked: {}", jti);
            return;
        }

        storedToken.revoke(reason == null ? RevocationReason.TOKEN_REFRESH : reason);
        if (replacedBy != null && !replacedBy.isBlank()) {
            storedToken.setReplacedBy(replacedBy);
        }
        refreshTokenRepository.save(storedToken);
        log.info("Refresh token revoked successfully: jti={} reason={} replacedBy={}", jti,
                storedToken.getRevocationReason(), storedToken.getReplacedBy());
    }

    private String createToken(User user, TokenType tokenType, long validDuration, String familyId) {

        try {
            JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS256);

            JWTClaimsSet jwtClaimsSet = buildJwtClaimsSet(user, tokenType, validDuration, familyId);

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

    private JWTClaimsSet parseClaims(String token, JwtErrorCode errorCode, String operation) {
        try {
            return SignedJWT.parse(token).getJWTClaimsSet();
        } catch (ParseException e) {
            log.error("Failed to parse JWT claims for {}", operation, e);
            throw new AppException(errorCode);
        }
    }

    private JWTClaimsSet getClaimsSet(SignedJWT signedJWT, JwtErrorCode errorCode, String operation) {
        try {
            return signedJWT.getJWTClaimsSet();
        } catch (ParseException e) {
            log.error("Failed to parse JWT claims for {}", operation, e);
            throw new AppException(errorCode);
        }
    }

    private String getStringClaim(JWTClaimsSet claimsSet, String claimName, String operation) {
        try {
            return claimsSet.getStringClaim(claimName);
        } catch (ParseException e) {
            log.error("Failed to read JWT claim '{}' for {}", claimName, operation, e);
            throw new AppException(JwtErrorCode.TOKEN_INVALID);
        }
    }

    JWTClaimsSet buildJwtClaimsSet(User user, TokenType tokenType, long validDuration, String familyId) {
        Instant now = Instant.now();
        Date issueTime = Date.from(now);
        Date expirationTime = Date.from(now.plus(validDuration, ChronoUnit.SECONDS));

        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder().subject(user.getUsername()).issuer(issuer)
                .issueTime(issueTime).expirationTime(expirationTime).jwtID(UUID.randomUUID().toString())
                .claim(TOKEN_TYPE_CLAIM, tokenType.name()).claim(USER_ID_CLAIM, user.getId())
                .claim(TOKEN_VERSION_CLAIM, user.getTokenVersion() != null ? user.getTokenVersion() : 0)
                .claim(SCOPE_CLAIM, buildRoles(user.getRoles()));

        if (familyId != null && tokenType == TokenType.REFRESH) {
            builder.claim(FAMILY_ID_CLAIM, familyId);
        }

        return builder.build();
    }

    List<String> buildRoles(Set<Role> roles) {
        if (CollectionUtils.isEmpty(roles)) {
            return List.of();
        }

        return roles.stream().filter(Objects::nonNull).map(Role::getCode) // "USER", "ADMIN"
                .filter(Objects::nonNull).map(Enum::name).map(code -> ROLE_PREFIX + code) // "ROLE_USER", "ROLE_ADMIN"
                .distinct().toList();
    }

    private byte[] getSignerKeyBytes() {
        if (signerKey == null || signerKey.isBlank()) {
            throw new IllegalStateException("JWT signer key is not configured");
        }
        return signerKey.getBytes(StandardCharsets.UTF_8);
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

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not found for token hashing", e);
            throw new AppException(JwtErrorCode.TOKEN_HASHING_FAILED);
        }
    }
}
