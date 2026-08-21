package com.tien.aivirabackend.config.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag")
public record RagProperties(Boolean enabled, String qdrantUrl, String qdrantApiKey, String collectionAlias,
        Integer candidateLimit, Double scoreThreshold, Integer indexBatchSize, Integer maxRetries, Boolean debugScores,
        Duration connectTimeout, Duration readTimeout, String geminiEmbeddingModel, Integer geminiEmbeddingDimensions,
        String openAiEmbeddingModel, Integer openAiEmbeddingDimensions) {
    public RagProperties {
        enabled = enabled != null && enabled;
        qdrantUrl = value(qdrantUrl, "http://localhost:6333");
        collectionAlias = value(collectionAlias, "aivira-books-current");
        candidateLimit = candidateLimit == null ? 100 : Math.max(10, candidateLimit);
        scoreThreshold = scoreThreshold == null ? 0.25 : scoreThreshold;
        indexBatchSize = indexBatchSize == null ? 25 : Math.max(1, indexBatchSize);
        maxRetries = maxRetries == null ? 5 : Math.max(0, maxRetries);
        debugScores = debugScores != null && debugScores;
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(5) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(20) : readTimeout;
        geminiEmbeddingModel = value(geminiEmbeddingModel, "gemini-embedding-001");
        geminiEmbeddingDimensions = geminiEmbeddingDimensions == null ? 768 : geminiEmbeddingDimensions;
        openAiEmbeddingModel = value(openAiEmbeddingModel, "text-embedding-3-small");
        openAiEmbeddingDimensions = openAiEmbeddingDimensions == null ? 1536 : openAiEmbeddingDimensions;
    }

    private static String value(String input, String fallback) {
        return input == null || input.isBlank() ? fallback : input;
    }
}

