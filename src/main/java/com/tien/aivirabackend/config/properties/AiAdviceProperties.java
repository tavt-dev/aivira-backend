package com.tien.aivirabackend.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai-advice")
public record AiAdviceProperties(String provider, Integer monthlyLimit, Integer pageSize, Integer candidateLimit,
        Integer resultLimit, Integer retentionDays, Boolean failFast) {
    public AiAdviceProperties {
        provider = defaultValue(provider, "gemini");
        monthlyLimit = monthlyLimit == null ? 30 : monthlyLimit;
        pageSize = pageSize == null ? 10 : pageSize;
        candidateLimit = candidateLimit == null ? 200 : candidateLimit;
        resultLimit = resultLimit == null ? 30 : resultLimit;
        retentionDays = retentionDays == null ? 30 : retentionDays;
        failFast = failFast != null && failFast;
    }

    private static String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
