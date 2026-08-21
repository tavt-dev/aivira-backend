package com.tien.aivirabackend.config;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import com.tien.aivirabackend.config.properties.RagProperties;
import com.tien.aivirabackend.service.ai.EmbeddingClient;
import com.tien.aivirabackend.service.ai.QdrantVectorStore;

import lombok.RequiredArgsConstructor;

@Component("rag")
@RequiredArgsConstructor
public class RagHealthIndicator implements HealthIndicator {
    private final RagProperties properties;
    private final EmbeddingClient embeddingClient;
    private final QdrantVectorStore qdrant;

    @Override
    public Health health() {
        if (!properties.enabled()) return Health.up().withDetail("enabled", false).build();
        boolean provider = embeddingClient.configured();
        boolean vectorStore = qdrant.healthy();
        return Health.up().withDetail("enabled", true).withDetail("operational", provider && vectorStore)
                .withDetail("embeddingProvider", embeddingClient.provider()).withDetail("embeddingModel", embeddingClient.model())
                .withDetail("qdrant", vectorStore ? "up" : "degraded").build();
    }
}
