package com.tien.aivirabackend.config.properties;

import jakarta.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "brevo")
public class BrevoProperties {
    private String apiKey = "";
    private String baseUrl = "https://api.brevo.com";
    private String sendEmailPath = "/v3/smtp/email";

    @Min(100)
    private int connectTimeoutMs = 5000;

    @Min(100)
    private int readTimeoutMs = 10000;

    public String sendEmailUrl() {
        return baseUrl.replaceAll("/+$", "") + "/" + sendEmailPath.replaceAll("^/+", "");
    }
}
