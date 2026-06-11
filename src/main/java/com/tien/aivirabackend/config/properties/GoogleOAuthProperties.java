package com.tien.aivirabackend.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "auth.google")
public class GoogleOAuthProperties {
    private boolean enabled = false;
    private String clientId = "";
    private String clientSecret = "";
    private String redirectUri = "";
    private String scopes = "openid email profile";
    private String frontendSuccessUrl = "http://localhost:5173/auth/google/success";
    private String frontendFailureUrl = "http://localhost:5173/auth/google/failure";
    private long stateTtlSeconds = 300;
    private long ticketTtlSeconds = 300;
    private String authorizationUri = "https://accounts.google.com/o/oauth2/v2/auth";
    private String tokenUri = "https://oauth2.googleapis.com/token";
    private String issuerUri = "https://accounts.google.com";
}
