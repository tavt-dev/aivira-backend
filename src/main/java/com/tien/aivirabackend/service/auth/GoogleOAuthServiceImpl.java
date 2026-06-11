package com.tien.aivirabackend.service.auth;

import static org.springframework.util.StringUtils.truncate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import com.tien.aivirabackend.config.properties.GoogleOAuthProperties;
import com.tien.aivirabackend.constant.PredefinedRole;
import com.tien.aivirabackend.constant.SignInProvider;
import com.tien.aivirabackend.domain.dto.response.AuthenticationResponse;
import com.tien.aivirabackend.domain.dto.response.GoogleOAuthAuthorizationResponse;
import com.tien.aivirabackend.domain.dto.response.GoogleOAuthCallbackResponse;
import com.tien.aivirabackend.domain.entity.user.OAuthLoginState;
import com.tien.aivirabackend.domain.entity.user.OAuthLoginTicket;
import com.tien.aivirabackend.domain.entity.user.Role;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.AccountErrorCode;
import com.tien.aivirabackend.exception.errorCode.GoogleOAuthErrorCode;
import com.tien.aivirabackend.exception.errorCode.UserErrorCode;
import com.tien.aivirabackend.repository.OAuthLoginStateRepository;
import com.tien.aivirabackend.repository.OAuthLoginTicketRepository;
import com.tien.aivirabackend.repository.RoleRepository;
import com.tien.aivirabackend.repository.UserRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j(topic = "GOOGLE-OAUTH-SERVICE")
public class GoogleOAuthServiceImpl implements GoogleOAuthService {
    private static final int RANDOM_BYTES = 32;

    GoogleOAuthProperties properties;
    OAuthLoginStateRepository stateRepository;
    OAuthLoginTicketRepository ticketRepository;
    UserRepository userRepository;
    RoleRepository roleRepository;
    JwtService jwtService;
    AccountAuthPolicy accountAuthPolicy;
    GoogleIdTokenVerifier idTokenVerifier;
    RestClient.Builder restClientBuilder;

    SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public GoogleOAuthAuthorizationResponse createAuthorization(String nextPath, String deviceInfo, String ipAddress) {
        requireConfigured();
        String state = randomToken();
        OAuthLoginState loginState = OAuthLoginState.builder()
                .stateHash(hash(state))
                .nextPath(sanitizeNextPath(nextPath))
                .deviceInfo(truncate(deviceInfo, 512))
                .ipAddress(truncate(ipAddress, 45))
                .expiresAt(Instant.now().plus(properties.getStateTtlSeconds(), ChronoUnit.SECONDS))
                .build();
        stateRepository.save(loginState);

        String authorizationUrl = UriComponentsBuilder.fromUriString(properties.getAuthorizationUri())
                .queryParam("client_id", properties.getClientId())
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", properties.getScopes())
                .queryParam("state", state)
                .queryParam("access_type", "online")
                .queryParam("include_granted_scopes", "true")
                .build()
                .encode()
                .toUriString();
        return GoogleOAuthAuthorizationResponse.builder()
                .authorizationUrl(authorizationUrl)
                .build();
    }

    @Override
    @Transactional
    public GoogleOAuthCallbackResponse handleCallback(String code, String state, String deviceInfo, String ipAddress) {
        requireConfigured();
        if (!StringUtils.hasText(code) || !StringUtils.hasText(state)) {
            throw new AppException(GoogleOAuthErrorCode.GOOGLE_OAUTH_CODE_INVALID);
        }

        OAuthLoginState loginState = consumeState(state);
        GoogleTokenResponse tokenResponse = exchangeCode(code);
        GoogleUserInfo googleUser = idTokenVerifier.verify(tokenResponse.idToken(), properties.getClientId());
        if (!googleUser.emailVerified()) {
            throw new AppException(GoogleOAuthErrorCode.GOOGLE_EMAIL_NOT_VERIFIED);
        }

        User user = resolveUser(googleUser);
        accountAuthPolicy.validateAccountForAuth(user);
        String ticket = createTicket(user, deviceInfo, ipAddress);
        log.info("google_oauth_login_ticket_created userId={} email={}", user.getId(), user.getEmail());

        return GoogleOAuthCallbackResponse.builder()
                .redirectUrl(successRedirectUrl(ticket, loginState.getNextPath()))
                .build();
    }

    @Override
    @Transactional
    public AuthenticationResponse exchangeTicket(String ticket, String deviceInfo, String ipAddress) {
        requireConfigured();
        OAuthLoginTicket loginTicket = ticketRepository
                .findByTicketHashForUpdate(hash(ticket))
                .orElseThrow(() -> new AppException(GoogleOAuthErrorCode.GOOGLE_LOGIN_TICKET_INVALID));
        Instant now = Instant.now();
        if (!loginTicket.isUsable(now)) {
            throw new AppException(GoogleOAuthErrorCode.GOOGLE_LOGIN_TICKET_INVALID);
        }
        loginTicket.setConsumedAt(now);
        ticketRepository.save(loginTicket);

        User user = loginTicket.getUser();
        accountAuthPolicy.validateAccountForAuth(user);
        String accessToken = jwtService.createAccessToken(user);
        String refreshToken = jwtService.createRefreshToken(user, deviceInfo, ipAddress, null);
        log.info("google_oauth_exchange_success userId={} email={}", user.getId(), user.getEmail());

        return AuthenticationResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .accessTokenExpiresIn(jwtService.getAccessTokenExpiresIn())
                .authenticated(true)
                .build();
    }

    @Override
    public String failureRedirectUrl(String errorCode) {
        return appendQuery(properties.getFrontendFailureUrl(), Map.of("error", safeQueryValue(errorCode)));
    }

    private OAuthLoginState consumeState(String state) {
        OAuthLoginState loginState = stateRepository
                .findByStateHashForUpdate(hash(state))
                .orElseThrow(() -> new AppException(GoogleOAuthErrorCode.GOOGLE_OAUTH_STATE_INVALID));
        Instant now = Instant.now();
        if (!loginState.isUsable(now)) {
            throw new AppException(GoogleOAuthErrorCode.GOOGLE_OAUTH_STATE_INVALID);
        }
        loginState.setConsumedAt(now);
        stateRepository.save(loginState);
        return loginState;
    }

    private GoogleTokenResponse exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        form.add("redirect_uri", properties.getRedirectUri());
        form.add("grant_type", "authorization_code");
        try {
            Map<String, Object> response = restClientBuilder
                    .build()
                    .post()
                    .uri(URI.create(properties.getTokenUri()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            String idToken = response == null ? null : stringValue(response.get("id_token"));
            if (!StringUtils.hasText(idToken)) {
                throw new AppException(GoogleOAuthErrorCode.GOOGLE_OAUTH_CODE_INVALID);
            }
            return new GoogleTokenResponse(idToken);
        } catch (RestClientException e) {
            throw new AppException(GoogleOAuthErrorCode.GOOGLE_OAUTH_CODE_INVALID, e);
        }
    }

    private User resolveUser(GoogleUserInfo googleUser) {
        return userRepository
                .findByProviderAndProviderUserId(SignInProvider.GOOGLE, googleUser.subject())
                .orElseGet(() -> userRepository
                        .findByEmail(googleUser.email())
                        .map(existing -> linkGoogleUser(existing, googleUser))
                        .orElseGet(() -> createGoogleUser(googleUser)));
    }

    private User linkGoogleUser(User user, GoogleUserInfo googleUser) {
        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new AppException(AccountErrorCode.ACCOUNT_DELETED);
        }
        user.setProviderUserId(googleUser.subject());
        user.setProvider(SignInProvider.GOOGLE);
        user.setEmailVerified(true);
        user.setIsActive(true);
        copyGoogleProfileIfMissing(user, googleUser);
        return userRepository.save(user);
    }

    private User createGoogleUser(GoogleUserInfo googleUser) {
        Role userRole = roleRepository
                .findByCode(PredefinedRole.USER)
                .orElseThrow(() -> new AppException(UserErrorCode.ROLE_NOT_FOUND));
        User user = User.builder()
                .username(generateUsername(googleUser))
                .email(googleUser.email())
                .password(null)
                .provider(SignInProvider.GOOGLE)
                .providerUserId(googleUser.subject())
                .emailVerified(true)
                .isActive(true)
                .isLocked(false)
                .isDeleted(false)
                .tokenVersion(0)
                .failedLoginAttempts(0)
                .build();
        copyGoogleProfileIfMissing(user, googleUser);
        user.getRoles().add(userRole);
        return userRepository.save(user);
    }

    private void copyGoogleProfileIfMissing(User user, GoogleUserInfo googleUser) {
        if (!StringUtils.hasText(user.getFirstName())) {
            user.setFirstName(truncate(googleUser.firstName(), 50));
        }
        if (!StringUtils.hasText(user.getLastName())) {
            user.setLastName(truncate(googleUser.lastName(), 50));
        }
        if (!StringUtils.hasText(user.getAvatarUrl())) {
            user.setAvatarUrl(googleUser.pictureUrl());
        }
    }

    private String createTicket(User user, String deviceInfo, String ipAddress) {
        String ticket = randomToken();
        OAuthLoginTicket loginTicket = OAuthLoginTicket.builder()
                .ticketHash(hash(ticket))
                .user(user)
                .deviceInfo(truncate(deviceInfo, 512))
                .ipAddress(truncate(ipAddress, 45))
                .expiresAt(Instant.now().plus(properties.getTicketTtlSeconds(), ChronoUnit.SECONDS))
                .build();
        ticketRepository.save(loginTicket);
        return ticket;
    }

    private String generateUsername(GoogleUserInfo googleUser) {
        String emailPrefix = googleUser.email().split("@", 2)[0].toLowerCase(Locale.ROOT);
        String base = emailPrefix.replaceAll("[^a-z0-9._-]", "");
        if (base.length() < 4) {
            base = "google_" + base;
        }
        base = truncate(base, 50);
        if (!userRepository.existsByUsername(base)) {
            return base;
        }
        String suffix = "-" + hash(googleUser.subject()).substring(0, 8);
        int maxBaseLength = 50 - suffix.length();
        return truncate(base, maxBaseLength) + suffix;
    }

    private String successRedirectUrl(String ticket, String nextPath) {
        return appendQuery(
                properties.getFrontendSuccessUrl(),
                Map.of("ticket", ticket, "next", StringUtils.hasText(nextPath) ? nextPath : "/"));
    }

    private String appendQuery(String baseUrl, Map<String, String> params) {
        StringBuilder builder = new StringBuilder(baseUrl);
        builder.append(baseUrl.contains("?") ? "&" : "?");
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                builder.append("&");
            }
            first = false;
            builder.append(encode(entry.getKey())).append("=").append(encode(entry.getValue()));
        }
        return builder.toString();
    }

    private String sanitizeNextPath(String nextPath) {
        if (!StringUtils.hasText(nextPath) || !nextPath.startsWith("/") || nextPath.startsWith("//")) {
            return "/";
        }
        return truncate(nextPath, 500);
    }

    private void requireConfigured() {
        if (!properties.isEnabled()) {
            throw new AppException(GoogleOAuthErrorCode.GOOGLE_OAUTH_DISABLED);
        }
        if (!StringUtils.hasText(properties.getClientId())
                || !StringUtils.hasText(properties.getClientSecret())
                || !StringUtils.hasText(properties.getRedirectUri())
                || !StringUtils.hasText(properties.getFrontendSuccessUrl())
                || !StringUtils.hasText(properties.getFrontendFailureUrl())
                || !StringUtils.hasText(properties.getAuthorizationUri())
                || !StringUtils.hasText(properties.getTokenUri())
                || !StringUtils.hasText(properties.getIssuerUri())) {
            throw new AppException(GoogleOAuthErrorCode.GOOGLE_OAUTH_CONFIG_INVALID);
        }
    }

    private String randomToken() {
        byte[] bytes = new byte[RANDOM_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                String part = Integer.toHexString(0xff & b);
                if (part.length() == 1) {
                    hex.append('0');
                }
                hex.append(part);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String safeQueryValue(String value) {
        return StringUtils.hasText(value) ? value : GoogleOAuthErrorCode.GOOGLE_OAUTH_CODE_INVALID.getCode();
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record GoogleTokenResponse(String idToken) {}
}
