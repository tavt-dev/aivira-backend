package com.tien.aivirabackend.service.auth;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

class RefreshTokenCookieServiceTest {

    private RefreshTokenCookieService refreshTokenCookieService;

    @BeforeEach
    void setUp() {
        refreshTokenCookieService = new RefreshTokenCookieService();
        ReflectionTestUtils.setField(refreshTokenCookieService, "cookieName", "refreshToken");
        ReflectionTestUtils.setField(refreshTokenCookieService, "cookiePath", "/api/v1/auth");
        ReflectionTestUtils.setField(refreshTokenCookieService, "cookieSameSite", "Lax");
        ReflectionTestUtils.setField(refreshTokenCookieService, "cookieSecure", true);
        ReflectionTestUtils.setField(refreshTokenCookieService, "cookieHttpOnly", true);
    }

    @Test
    void writeRefreshTokenCookie_shouldSetCookieWithExpectedAttributes() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        refreshTokenCookieService.writeRefreshTokenCookie(response, "refresh-token-value", 36000);

        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).contains("refreshToken=refresh-token-value");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("Secure");
        assertThat(setCookie).contains("SameSite=Lax");
        assertThat(setCookie).contains("Path=/api/v1/auth");
    }

    @Test
    void clearRefreshTokenCookie_shouldSetCookieMaxAgeZero() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        refreshTokenCookieService.clearRefreshTokenCookie(response);

        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).contains("refreshToken=");
        assertThat(setCookie).contains("Max-Age=0");
    }

    @Test
    void extractRefreshToken_shouldPreferConfiguredCookieName() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("otherCookie", "abc"), new Cookie("refreshToken", "token-from-cookie"));

        String refreshToken = refreshTokenCookieService.extractRefreshToken(request);

        assertThat(refreshToken).isEqualTo("token-from-cookie");
    }

    @Test
    void resolveRefreshToken_shouldPreferCookieOverRequestBody() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refreshToken", "token-from-cookie"));

        assertThat(refreshTokenCookieService.resolveRefreshToken(request, "token-from-body", true))
                .contains("token-from-cookie");
    }

    @Test
    void resolveRefreshToken_shouldUseRequestBodyWhenEnabledAndCookieIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(refreshTokenCookieService.resolveRefreshToken(request, "token-from-body", true))
                .contains("token-from-body");
    }

    @Test
    void resolveRefreshToken_shouldIgnoreRequestBodyWhenDisabled() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(refreshTokenCookieService.resolveRefreshToken(request, "token-from-body", false)).isEmpty();
    }
}
