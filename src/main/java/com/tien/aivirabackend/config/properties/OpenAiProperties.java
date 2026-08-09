package com.tien.aivirabackend.config.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(
        String apiKey,
        String baseUrl,
        String model,
        Duration connectTimeout,
        Duration readTimeout,
        Integer monthlyLimit,
        Integer pageSize,
        Integer retentionDays) {
    public OpenAiProperties {
        baseUrl = defaultValue(baseUrl, "https://api.openai.com/v1");
        model = defaultValue(model, "gpt-5.6-luna");
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(5) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(45) : readTimeout;
        monthlyLimit = monthlyLimit == null ? 30 : monthlyLimit;
        pageSize = pageSize == null ? 10 : pageSize;
        retentionDays = retentionDays == null ? 30 : retentionDays;
    }

    private static String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
