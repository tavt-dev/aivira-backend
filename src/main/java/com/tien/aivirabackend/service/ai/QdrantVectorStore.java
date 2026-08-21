package com.tien.aivirabackend.service.ai;

import java.util.*;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.tien.aivirabackend.config.properties.RagProperties;

import tools.jackson.databind.JsonNode;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class QdrantVectorStore {
    public record Match(long productId, double score) {}
    private final RestClient client;
    private final RagProperties properties;
    private final MeterRegistry metrics;

    public QdrantVectorStore(@Qualifier("qdrantRestClient") RestClient client, RagProperties properties,
            MeterRegistry metrics) {
        this.client = client; this.properties = properties; this.metrics = metrics;
    }

    public String collectionName(EmbeddingClient embedding) {
        return sanitize(properties.collectionAlias() + "-" + embedding.provider() + "-" + embedding.model() + "-" + embedding.dimension());
    }

    public void ensureCollection(EmbeddingClient embedding) {
        String collection = collectionName(embedding);
        try { client.get().uri("/collections/{name}", collection).retrieve().toBodilessEntity(); }
        catch (RuntimeException missing) {
            client.put().uri("/collections/{name}", collection)
                    .body(Map.of("vectors", Map.of("size", embedding.dimension(), "distance", "Cosine")))
                    .retrieve().toBodilessEntity();
        }
    }

    public void upsert(EmbeddingClient embedding, long productId, List<Double> vector, Map<String, Object> payload) {
        ensureCollection(embedding);
        client.put().uri("/collections/{name}/points?wait=true", collectionName(embedding))
                .body(Map.of("points", List.of(Map.of("id", productId, "vector", vector, "payload", payload))))
                .retrieve().toBodilessEntity();
    }

    public void updatePayload(EmbeddingClient embedding, long productId, Map<String, Object> payload) {
        client.post().uri("/collections/{name}/points/payload?wait=true", collectionName(embedding))
                .body(Map.of("payload", payload, "points", List.of(productId))).retrieve().toBodilessEntity();
    }

    public boolean pointExists(EmbeddingClient embedding, long productId) {
        try {
            client.get().uri("/collections/{name}/points/{id}", collectionName(embedding), productId)
                    .retrieve().toBodilessEntity();
            return true;
        } catch (RuntimeException ex) { return false; }
    }

    public void activateAlias(EmbeddingClient embedding) {
        ensureCollection(embedding);
        try {
            client.post().uri("/collections/aliases").body(Map.of("actions", List.of(
                    Map.of("delete_alias", Map.of("alias_name", properties.collectionAlias())),
                    Map.of("create_alias", Map.of("collection_name", collectionName(embedding),
                            "alias_name", properties.collectionAlias()))))).retrieve().toBodilessEntity();
        } catch (RuntimeException firstAlias) {
            client.post().uri("/collections/aliases").body(Map.of("actions", List.of(
                    Map.of("create_alias", Map.of("collection_name", collectionName(embedding),
                            "alias_name", properties.collectionAlias()))))).retrieve().toBodilessEntity();
        }
    }

    public void delete(EmbeddingClient embedding, long productId) {
        ensureCollection(embedding);
        client.post().uri("/collections/{name}/points/delete?wait=true", collectionName(embedding))
                .body(Map.of("points", List.of(productId))).retrieve().toBodilessEntity();
    }

    public List<Match> search(EmbeddingClient embedding, List<Double> vector, int limit) {
        Timer.Sample sample = Timer.start(metrics);
        ensureCollection(embedding);
        Map<String, Object> body = Map.of("vector", vector, "limit", limit, "score_threshold",
                properties.scoreThreshold(), "with_payload", true, "with_vector", false);
        JsonNode root = client.post().uri("/collections/{name}/points/search", collectionName(embedding))
                .body(body).retrieve().body(JsonNode.class);
        if (root == null || !root.path("result").isArray()) {
            sample.stop(metrics.timer("rag.qdrant.search.latency"));
            return List.of();
        }
        List<Match> matches = new ArrayList<>();
        root.path("result").forEach(item -> {
            JsonNode productId = item.path("payload").path("productId");
            if (productId.canConvertToLong()) matches.add(new Match(productId.asLong(), item.path("score").asDouble()));
        });
        sample.stop(metrics.timer("rag.qdrant.search.latency"));
        return matches;
    }

    public boolean healthy() {
        try { client.get().uri("/healthz").retrieve().toBodilessEntity(); return true; }
        catch (RuntimeException ex) { return false; }
    }

    private String sanitize(String value) { return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-"); }
}
