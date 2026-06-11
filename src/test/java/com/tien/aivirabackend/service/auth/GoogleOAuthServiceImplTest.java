package com.tien.aivirabackend.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.tien.aivirabackend.config.properties.GoogleOAuthProperties;
import com.tien.aivirabackend.constant.PredefinedRole;
import com.tien.aivirabackend.constant.SignInProvider;
import com.tien.aivirabackend.domain.entity.user.OAuthLoginState;
import com.tien.aivirabackend.domain.entity.user.OAuthLoginTicket;
import com.tien.aivirabackend.domain.entity.user.Role;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.GoogleOAuthErrorCode;
import com.tien.aivirabackend.repository.OAuthLoginStateRepository;
import com.tien.aivirabackend.repository.OAuthLoginTicketRepository;
import com.tien.aivirabackend.repository.RoleRepository;
import com.tien.aivirabackend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class GoogleOAuthServiceImplTest {
    @Mock
    OAuthLoginStateRepository stateRepository;

    @Mock
    OAuthLoginTicketRepository ticketRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    RoleRepository roleRepository;

    @Mock
    JwtService jwtService;

    @Mock
    GoogleIdTokenVerifier idTokenVerifier;

    GoogleOAuthProperties properties;
    GoogleOAuthServiceImpl service;
    MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        properties = new GoogleOAuthProperties();
        properties.setEnabled(true);
        properties.setClientId("google-client-id");
        properties.setClientSecret("google-secret");
        properties.setRedirectUri("http://localhost:8080/api/v1/auth/google/callback");
        properties.setFrontendSuccessUrl("http://localhost:5173/auth/google/success");
        properties.setFrontendFailureUrl("http://localhost:5173/auth/google/failure");
        properties.setTokenUri("https://oauth2.googleapis.com/token");

        AccountAuthPolicy accountAuthPolicy = new AccountAuthPolicy(userRepository);
        ReflectionTestUtils.setField(accountAuthPolicy, "maxFailedLoginAttempts", 5);
        ReflectionTestUtils.setField(accountAuthPolicy, "failedLoginWindowMinutes", 15);
        ReflectionTestUtils.setField(accountAuthPolicy, "lockMinutes", 15);

        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new GoogleOAuthServiceImpl(
                properties,
                stateRepository,
                ticketRepository,
                userRepository,
                roleRepository,
                jwtService,
                accountAuthPolicy,
                idTokenVerifier,
                builder);
    }

    @Test
    void createAuthorization_shouldPersistHashedStateAndReturnGoogleUrl() {
        var response = service.createAuthorization("/cart", "ua", "127.0.0.1");

        assertThat(response.getAuthorizationUrl()).contains("accounts.google.com");
        assertThat(response.getAuthorizationUrl()).contains("client_id=google-client-id");
        assertThat(response.getAuthorizationUrl()).contains("response_type=code");
        ArgumentCaptor<OAuthLoginState> captor = ArgumentCaptor.forClass(OAuthLoginState.class);
        verify(stateRepository).save(captor.capture());
        assertThat(captor.getValue().getStateHash()).hasSize(64);
        assertThat(captor.getValue().getNextPath()).isEqualTo("/cart");
        assertThat(captor.getValue().getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void handleCallback_whenGoogleUserIsNew_shouldCreateUserAndTicket() {
        OAuthLoginState state = OAuthLoginState.builder()
                .stateHash("hash")
                .nextPath("/orders")
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        when(stateRepository.findByStateHashForUpdate(anyString())).thenReturn(Optional.of(state));
        server.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("grant_type=authorization_code")))
                .andRespond(withSuccess("{\"id_token\":\"google-id-token\"}", MediaType.APPLICATION_JSON));
        when(idTokenVerifier.verify("google-id-token", "google-client-id"))
                .thenReturn(new GoogleUserInfo(
                        "google-sub", "reader@example.com", true, "Aivira", "Reader", "https://avatar.example/a.png"));
        when(userRepository.findByProviderAndProviderUserId(SignInProvider.GOOGLE, "google-sub"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("reader@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("reader")).thenReturn(false);
        when(roleRepository.findByCode(PredefinedRole.USER))
                .thenReturn(Optional.of(
                        Role.builder().id(1L).code(PredefinedRole.USER).build()));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user-1");
            return user;
        });

        var callback = service.handleCallback("code", "state", "ua", "127.0.0.1");

        assertThat(state.getConsumedAt()).isNotNull();
        assertThat(callback.getRedirectUrl()).contains("ticket=");
        assertThat(callback.getRedirectUrl()).contains("next=%2Forders");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getProvider()).isEqualTo(SignInProvider.GOOGLE);
        assertThat(userCaptor.getValue().getEmailVerified()).isTrue();
        assertThat(userCaptor.getValue().getPassword()).isNull();
        ArgumentCaptor<OAuthLoginTicket> ticketCaptor = ArgumentCaptor.forClass(OAuthLoginTicket.class);
        verify(ticketRepository).save(ticketCaptor.capture());
        assertThat(ticketCaptor.getValue().getTicketHash()).hasSize(64);
        server.verify();
    }

    @Test
    void handleCallback_whenEmailNotVerified_shouldFail() {
        OAuthLoginState state = OAuthLoginState.builder()
                .stateHash("hash")
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        when(stateRepository.findByStateHashForUpdate(anyString())).thenReturn(Optional.of(state));
        server.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andRespond(withSuccess("{\"id_token\":\"google-id-token\"}", MediaType.APPLICATION_JSON));
        when(idTokenVerifier.verify("google-id-token", "google-client-id"))
                .thenReturn(new GoogleUserInfo("google-sub", "reader@example.com", false, null, null, null));

        assertThatThrownBy(() -> service.handleCallback("code", "state", "ua", "127.0.0.1"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(GoogleOAuthErrorCode.GOOGLE_EMAIL_NOT_VERIFIED));
    }

    @Test
    void handleCallback_whenEmailMatchesLocalUser_shouldLinkGoogleAndKeepPassword() {
        OAuthLoginState state = OAuthLoginState.builder()
                .stateHash("hash")
                .nextPath("/")
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        User localUser = buildActiveUser();
        localUser.setProvider(SignInProvider.LOCAL);
        localUser.setProviderUserId(null);
        localUser.setPassword("hashed-password");
        when(stateRepository.findByStateHashForUpdate(anyString())).thenReturn(Optional.of(state));
        server.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andRespond(withSuccess("{\"id_token\":\"google-id-token\"}", MediaType.APPLICATION_JSON));
        when(idTokenVerifier.verify("google-id-token", "google-client-id"))
                .thenReturn(new GoogleUserInfo("google-sub", "reader@example.com", true, "Aivira", "Reader", null));
        when(userRepository.findByProviderAndProviderUserId(SignInProvider.GOOGLE, "google-sub"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("reader@example.com")).thenReturn(Optional.of(localUser));
        when(userRepository.save(localUser)).thenReturn(localUser);

        service.handleCallback("code", "state", "ua", "127.0.0.1");

        assertThat(localUser.getProvider()).isEqualTo(SignInProvider.GOOGLE);
        assertThat(localUser.getProviderUserId()).isEqualTo("google-sub");
        assertThat(localUser.getPassword()).isEqualTo("hashed-password");
        assertThat(localUser.getEmailVerified()).isTrue();
        verify(ticketRepository).save(any(OAuthLoginTicket.class));
    }

    @Test
    void exchangeTicket_shouldConsumeTicketAndReturnAiviraTokens() {
        User user = buildActiveUser();
        OAuthLoginTicket ticket = OAuthLoginTicket.builder()
                .ticketHash("hash")
                .user(user)
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        when(ticketRepository.findByTicketHashForUpdate(anyString())).thenReturn(Optional.of(ticket));
        when(jwtService.createAccessToken(user)).thenReturn("access-token");
        when(jwtService.createRefreshToken(user, "ua", "127.0.0.1", null)).thenReturn("refresh-token");
        when(jwtService.getAccessTokenExpiresIn()).thenReturn(3600L);

        var response = service.exchangeTicket("ticket", "ua", "127.0.0.1");

        assertThat(response.getToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.isAuthenticated()).isTrue();
        assertThat(ticket.getConsumedAt()).isNotNull();
        verify(ticketRepository).save(ticket);
    }

    private User buildActiveUser() {
        User user = new User();
        user.setId("user-1");
        user.setUsername("reader");
        user.setEmail("reader@example.com");
        user.setProvider(SignInProvider.GOOGLE);
        user.setProviderUserId("google-sub");
        user.setEmailVerified(true);
        user.setIsActive(true);
        user.setIsLocked(false);
        user.setIsDeleted(false);
        user.setTokenVersion(0);
        user.setFailedLoginAttempts(0);
        return user;
    }
}
