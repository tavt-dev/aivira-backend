package com.tien.aivirabackend.config.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(String apiKey, String baseUrl, String model, Duration connectTimeout,
        Duration readTimeout) {
    public GeminiProperties {
        baseUrl = defaultValue(baseUrl, "https://generativelanguage.googleapis.com");
        model = defaultValue(model, "gemini-3.5-flash");
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(5) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(45) : readTimeout;
    }

    private static String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
