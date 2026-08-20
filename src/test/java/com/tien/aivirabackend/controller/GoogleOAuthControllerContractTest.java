package com.tien.aivirabackend.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.tien.aivirabackend.domain.dto.response.AuthenticationResponse;
import com.tien.aivirabackend.domain.dto.response.GoogleOAuthAuthorizationResponse;
import com.tien.aivirabackend.domain.dto.response.GoogleOAuthCallbackResponse;
import com.tien.aivirabackend.exception.GlobalExceptionHandler;
import com.tien.aivirabackend.service.auth.GoogleOAuthService;
import com.tien.aivirabackend.service.auth.RefreshTokenCookieService;

@ExtendWith(MockitoExtension.class)
class GoogleOAuthControllerContractTest {
    @Mock
    GoogleOAuthService googleOAuthService;

    @Mock
    RefreshTokenCookieService refreshTokenCookieService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        GoogleOAuthController controller = new GoogleOAuthController(googleOAuthService, refreshTokenCookieService);
        ReflectionTestUtils.setField(controller, "refreshTokenBodyEnabled", true);
        ReflectionTestUtils.setField(controller, "refreshTokenDuration", 36000L);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void authorize_shouldRedirectToGoogleAuthorizationUrl() throws Exception {
        when(googleOAuthService.createAuthorization(anyString(), anyString(), anyString()))
                .thenReturn(GoogleOAuthAuthorizationResponse.builder()
                        .authorizationUrl("https://accounts.google.com/o/oauth2/v2/auth?state=abc").build());

        mockMvc.perform(get("/auth/google/authorize").param("next", "/cart").header("User-Agent", "ua"))
                .andExpect(status().isFound()).andExpect(header().string(HttpHeaders.LOCATION,
                        "https://accounts.google.com/o/oauth2/v2/auth?state=abc"));

        verify(googleOAuthService).createAuthorization("/cart", "ua", "127.0.0.1");
    }

    @Test
    void callback_shouldRedirectToFrontendTicketUrl() throws Exception {
        when(googleOAuthService.handleCallback("code", "state", "ua", "127.0.0.1"))
                .thenReturn(GoogleOAuthCallbackResponse.builder()
                        .redirectUrl("http://localhost:5173/auth/google/success?ticket=ticket").build());

        mockMvc.perform(
                get("/auth/google/callback").param("code", "code").param("state", "state").header("User-Agent", "ua"))
                .andExpect(status().isFound()).andExpect(header().string(HttpHeaders.LOCATION,
                        "http://localhost:5173/auth/google/success?ticket=ticket"));
    }

    @Test
    void exchangeTicket_shouldReturnAuthenticationResponseAndWriteRefreshCookie() throws Exception {
        AuthenticationResponse response = AuthenticationResponse.builder().token("access-token")
                .refreshToken("refresh-token").tokenType("Bearer").accessTokenExpiresIn(3600).authenticated(true)
                .build();
        when(googleOAuthService.exchangeTicket("ticket", "ua", "127.0.0.1")).thenReturn(response);

        mockMvc.perform(post("/auth/google/exchange-ticket").contentType(MediaType.APPLICATION_JSON)
                .header("User-Agent", "ua").content("{\"ticket\":\"ticket\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));

        verify(refreshTokenCookieService).writeRefreshTokenCookie(
                org.mockito.ArgumentMatchers.any(HttpServletResponse.class), eq("refresh-token"), eq(36000L));
    }
}
