package com.tien.aivirabackend.service.ai;

import java.util.*;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.tien.aivirabackend.config.properties.GeminiProperties;
import com.tien.aivirabackend.config.properties.RagProperties;

import tools.jackson.databind.JsonNode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
@ConditionalOnProperty(name = "ai-advice.provider", havingValue = "gemini", matchIfMissing = true)
public class GeminiEmbeddingClient implements EmbeddingClient {
    private final RestClient client;
    private final GeminiProperties gemini;
    private final RagProperties rag;
    private final MeterRegistry metrics;

    public GeminiEmbeddingClient(@Qualifier("geminiRestClient") RestClient client, GeminiProperties gemini,
            RagProperties rag, MeterRegistry metrics) {
        this.client = client; this.gemini = gemini; this.rag = rag; this.metrics = metrics;
    }

    public List<Double> embedDocument(String title, String content) {
        return embed(content, "RETRIEVAL_DOCUMENT", title);
    }
    public List<Double> embedQuery(String content) { return embed(content, "RETRIEVAL_QUERY", null); }

    private List<Double> embed(String content, String taskType, String title) {
        if (!configured()) throw new IllegalStateException("GEMINI_API_KEY is missing");
        Timer.Sample sample = Timer.start(metrics);
        try {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("taskType", taskType);
        config.put("outputDimensionality", dimension());
        if (title != null && !title.isBlank()) config.put("title", title);
        Map<String, Object> body = Map.of("content", Map.of("parts", List.of(Map.of("text", content))),
                "embedContentConfig", config);
        JsonNode root = client.post().uri(uri -> uri.path("/v1beta/models/{model}:embedContent")
                .queryParam("key", gemini.apiKey()).build(model())).body(body).retrieve().body(JsonNode.class);
        JsonNode vector = root == null ? null : root.path("embedding").path("values");
        if (vector == null || !vector.isArray()) throw new IllegalStateException("Empty Gemini embedding response");
        List<Double> values = new ArrayList<>();
        vector.forEach(node -> values.add(node.asDouble()));
        return EmbeddingVectors.normalize(values, dimension());
        } finally { sample.stop(metrics.timer("rag.embedding.latency", "provider", provider())); }
    }

    public String provider() { return "gemini"; }
    public String model() { return rag.geminiEmbeddingModel(); }
    public int dimension() { return rag.geminiEmbeddingDimensions(); }
    public boolean configured() { return gemini.apiKey() != null && !gemini.apiKey().isBlank(); }
}
