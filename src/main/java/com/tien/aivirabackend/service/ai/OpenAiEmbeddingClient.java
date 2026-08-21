package com.tien.aivirabackend.service.ai;

import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.tien.aivirabackend.config.properties.OpenAiProperties;
import com.tien.aivirabackend.config.properties.RagProperties;

import tools.jackson.databind.JsonNode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
@ConditionalOnProperty(name = "ai-advice.provider", havingValue = "openai")
public class OpenAiEmbeddingClient implements EmbeddingClient {
    private final RestClient client;
    private final OpenAiProperties openAi;
    private final RagProperties rag;
    private final MeterRegistry metrics;

    public OpenAiEmbeddingClient(@Qualifier("openAiRestClient") RestClient client, OpenAiProperties openAi,
            RagProperties rag, MeterRegistry metrics) {
        this.client = client; this.openAi = openAi; this.rag = rag; this.metrics = metrics;
    }

    public List<Double> embedDocument(String title, String content) { return embed("Title: " + title + "\n" + content); }
    public List<Double> embedQuery(String content) { return embed(content); }

    private List<Double> embed(String input) {
        if (!configured()) throw new IllegalStateException("OPENAI_API_KEY is missing");
        Timer.Sample sample = Timer.start(metrics);
        try {
            JsonNode root = client.post().uri("/embeddings")
                    .body(Map.of("model", model(), "input", input, "dimensions", dimension(), "encoding_format", "float"))
                    .retrieve().body(JsonNode.class);
            if (root == null || root.path("data").isEmpty()) throw new IllegalStateException("Empty OpenAI embedding response");
            List<Double> values = new java.util.ArrayList<>();
            root.path("data").get(0).path("embedding").forEach(node -> values.add(node.asDouble()));
            return EmbeddingVectors.normalize(values, dimension());
        } finally { sample.stop(metrics.timer("rag.embedding.latency", "provider", provider())); }
    }

    public String provider() { return "openai"; }
    public String model() { return rag.openAiEmbeddingModel(); }
    public int dimension() { return rag.openAiEmbeddingDimensions(); }
    public boolean configured() { return openAi.apiKey() != null && !openAi.apiKey().isBlank(); }
}
