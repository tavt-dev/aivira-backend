package com.tien.aivirabackend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import com.nimbusds.jwt.SignedJWT;
import com.tien.aivirabackend.constant.OtpType;
import com.tien.aivirabackend.constant.RevocationReason;
import com.tien.aivirabackend.domain.entity.RefreshToken;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.entity.user.UserOtp;
import com.tien.aivirabackend.repository.RefreshTokenRepository;
import com.tien.aivirabackend.repository.UserOtpRepository;
import com.tien.aivirabackend.repository.UserRepository;

import tools.jackson.databind.JsonNode;

class AuthProfileSessionIntegrationTest extends AbstractIntegrationTest {
    private static final String PASSWORD = "Password123!";

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserOtpRepository userOtpRepository;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @Test
    void registerVerifyAndLogin_shouldCreateActiveUserAndIssueTokens() throws Exception {
        register("alice", "alice@example.com");

        User user = userRepository.findByUsername("alice").orElseThrow();
        assertThat(user.getEmailVerified()).isFalse();
        assertThat(user.getIsActive()).isFalse();

        verifyUser(user);

        User verifiedUser = userRepository.findByUsername("alice").orElseThrow();
        assertThat(verifiedUser.getEmailVerified()).isTrue();
        assertThat(verifiedUser.getIsActive()).isTrue();

        MvcResult login = login("alice", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        String accessToken = read(login, "/data/token").asText();
        String refreshToken = read(login, "/data/refreshToken").asText();
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();
        assertThat(refreshTokenRepository.countActiveSessionsByUserId(user.getId(), java.time.Instant.now()))
                .isEqualTo(1);
    }

    @Test
    void login_shouldRejectUnverifiedAccount() throws Exception {
        register("unverified", "unverified@example.com");

        login("unverified", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void refreshToken_shouldRotateAndDetectReuse() throws Exception {
        registerAndVerify("rotator", "rotator@example.com");
        String firstRefreshToken = read(login("rotator", PASSWORD).andReturn(), "/data/refreshToken")
                .asText();

        MvcResult refreshResult = mockMvc.perform(post("/auth/refresh-token")
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", firstRefreshToken))))
                .andExpect(status().isOk())
                .andReturn();

        String secondRefreshToken = read(refreshResult, "/data/refreshToken").asText();
        String firstJti = jwtId(firstRefreshToken);
        String secondJti = jwtId(secondRefreshToken);
        String familyId = jwtFamilyId(firstRefreshToken);

        RefreshToken firstStoredToken =
                refreshTokenRepository.findByJti(firstJti).orElseThrow();
        RefreshToken secondStoredToken =
                refreshTokenRepository.findByJti(secondJti).orElseThrow();

        assertThat(firstStoredToken.isRevoked()).isTrue();
        assertThat(firstStoredToken.getRevocationReason()).isEqualTo(RevocationReason.TOKEN_REFRESH);
        assertThat(firstStoredToken.getReplacedBy()).isEqualTo(secondJti);
        assertThat(secondStoredToken.isRevoked()).isFalse();
        assertThat(secondStoredToken.getFamilyId()).isEqualTo(familyId);

        mockMvc.perform(post("/auth/refresh-token")
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", firstRefreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("E2106"));

        assertThat(refreshTokenRepository.findByJti(secondJti).orElseThrow().isRevoked())
                .isTrue();
        assertThat(refreshTokenRepository.findByJti(secondJti).orElseThrow().getRevocationReason())
                .isEqualTo(RevocationReason.SECURITY_BREACH);
    }

    @Test
    void logout_shouldRevokeCurrentRefreshTokenAndClearCookie() throws Exception {
        registerAndVerify("logout_user", "logout@example.com");
        String refreshToken = read(login("logout_user", PASSWORD).andReturn(), "/data/refreshToken")
                .asText();
        String jti = jwtId(refreshToken);

        mockMvc.perform(post("/auth/logout")
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("refreshToken", 0));

        assertThat(refreshTokenRepository.findByJti(jti).orElseThrow().isRevoked())
                .isTrue();
    }

    @Test
    void protectedProfileEndpoints_shouldRequireTokenAndAllowCurrentUserUpdates() throws Exception {
        registerAndVerify("profile_user", "profile@example.com");
        String accessToken =
                read(login("profile_user", PASSWORD).andReturn(), "/data/token").asText();

        mockMvc.perform(get("/users/me")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("profile_user"));

        mockMvc.perform(put("/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("firstName", "Updated", "lastName", "User", "gender", "MALE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Updated"))
                .andExpect(jsonPath("$.data.gender").value("MALE"));
    }

    @Test
    void sessions_shouldListAndRevokeOnlyCurrentUserSession() throws Exception {
        registerAndVerify("session_user", "session@example.com");
        MvcResult login = login("session_user", PASSWORD).andReturn();
        String accessToken = read(login, "/data/token").asText();

        MvcResult sessions = mockMvc.perform(get("/auth/sessions").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sessionId").isNotEmpty())
                .andReturn();

        String sessionId = read(sessions, "/data/0/sessionId").asText();

        mockMvc.perform(delete("/auth/sessions/{sessionId}", sessionId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        assertThat(refreshTokenRepository.findById(sessionId).orElseThrow().isRevoked())
                .isTrue();
    }

    @Test
    void logoutAllAndChangePassword_shouldInvalidateOldAccessTokenByTokenVersion() throws Exception {
        registerAndVerify("version_user", "version@example.com");
        MvcResult firstLogin = login("version_user", PASSWORD).andReturn();
        String firstAccessToken = read(firstLogin, "/data/token").asText();

        mockMvc.perform(post("/auth/logout-all").header("Authorization", "Bearer " + firstAccessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + firstAccessToken))
                .andExpect(status().isUnauthorized());

        String secondAccessToken =
                read(login("version_user", PASSWORD).andReturn(), "/data/token").asText();

        mockMvc.perform(put("/users/me/password")
                        .header("Authorization", "Bearer " + secondAccessToken)
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of(
                                "currentPassword", PASSWORD,
                                "newPassword", "NewPassword123!",
                                "confirmPassword", "NewPassword123!"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + secondAccessToken))
                .andExpect(status().isUnauthorized());
    }

    private void registerAndVerify(String username, String email) throws Exception {
        register(username, email);
        verifyUser(userRepository.findByUsername(username).orElseThrow());
    }

    private void register(String username, String email) throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", username,
                                "password", PASSWORD,
                                "email", email,
                                "firstName", "Test",
                                "lastName", "User"))))
                .andExpect(status().isCreated());
    }

    private void verifyUser(User user) throws Exception {
        UserOtp otp = userOtpRepository
                .findTopByUserAndOtpTypeAndUsedFalseOrderByCreatedAtDesc(user, OtpType.REGISTER)
                .orElseThrow();

        mockMvc.perform(post("/auth/verify-user")
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("email", user.getEmail(), "otpCode", otp.getOtpCode()))))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions login(String username, String password)
            throws Exception {
        return mockMvc.perform(post("/auth/token")
                .contentType(APPLICATION_JSON)
                .content(json(Map.of("username", username, "password", password))));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode read(MvcResult result, String pointer) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).at(pointer);
    }

    private String jwtId(String token) throws Exception {
        return SignedJWT.parse(token).getJWTClaimsSet().getJWTID();
    }

    private String jwtFamilyId(String token) throws Exception {
        return SignedJWT.parse(token).getJWTClaimsSet().getStringClaim("family_id");
    }
}
