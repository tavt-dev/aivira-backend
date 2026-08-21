package com.tien.aivirabackend.service.ai;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tien.aivirabackend.config.properties.AiAdviceProperties;
import com.tien.aivirabackend.config.properties.RagProperties;
import com.tien.aivirabackend.constant.ProductStatus;
import com.tien.aivirabackend.constant.RetrievalMode;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.repository.ProductRepository;

import lombok.extern.slf4j.Slf4j;
import io.micrometer.core.instrument.MeterRegistry;

@Component
@Slf4j
public class HybridCatalogRetriever {
    public record Result(List<Product> products, RetrievalMode mode, String embeddingProvider, String embeddingModel,
            Map<Long, Double> semanticScores, Map<Long, Double> lexicalScores, Map<Long, Double> finalScores) {}

    private final AiCatalogRanker lexicalRanker;
    private final ProductRepository products;
    private final EmbeddingClient embeddings;
    private final QdrantVectorStore qdrant;
    private final RagProperties rag;
    private final AiAdviceProperties advice;
    private final MeterRegistry metrics;

    public HybridCatalogRetriever(AiCatalogRanker lexicalRanker, ProductRepository products,
            EmbeddingClient embeddings, QdrantVectorStore qdrant, RagProperties rag, AiAdviceProperties advice,
            MeterRegistry metrics) {
        this.lexicalRanker = lexicalRanker; this.products = products; this.embeddings = embeddings;
        this.qdrant = qdrant; this.rag = rag; this.advice = advice; this.metrics = metrics;
    }

    public Result retrieve(AiSearchProfile profile, String query, Set<Long> purchased) {
        List<Product> lexical = lexicalRanker.rank(profile, purchased);
        if (!rag.enabled() || !embeddings.configured()) return lexicalFallback(lexical);
        try {
            List<QdrantVectorStore.Match> semantic = qdrant.search(embeddings, embeddings.embedQuery(query),
                    rag.candidateLimit());
            Map<Long, Product> available = products.findByIdIn(semantic.stream().map(QdrantVectorStore.Match::productId)
                    .toList()).stream().filter(product -> valid(product, profile)).collect(Collectors.toMap(Product::getId,
                            Function.identity()));
            Map<Long, Double> semanticScores = semantic.stream().filter(match -> available.containsKey(match.productId()))
                    .collect(Collectors.toMap(QdrantVectorStore.Match::productId, QdrantVectorStore.Match::score,
                            Math::max, LinkedHashMap::new));
            Map<Long, Double> lexicalScores = rankScores(lexical);
            Map<Long, Product> all = new LinkedHashMap<>(available);
            lexical.forEach(product -> all.putIfAbsent(product.getId(), product));
            Map<Long, Double> finals = new HashMap<>();
            List<Long> semanticOrder = new ArrayList<>(semanticScores.keySet());
            for (int i = 0; i < semanticOrder.size(); i++) finals.merge(semanticOrder.get(i), 0.65 / (60 + i + 1), Double::sum);
            for (int i = 0; i < lexical.size(); i++) finals.merge(lexical.get(i).getId(), 0.25 / (60 + i + 1), Double::sum);
            all.values().forEach(product -> finals.merge(product.getId(), 0.10 * popularity(product), Double::sum));
            purchased.forEach(id -> finals.computeIfPresent(id, (key, score) -> score - 1));
            List<Product> ranked = all.values().stream().filter(product -> valid(product, profile))
                    .sorted(Comparator.comparingDouble((Product product) -> finals.getOrDefault(product.getId(), 0d))
                            .reversed().thenComparing(Product::getId)).limit(advice.resultLimit()).toList();
            RetrievalMode mode = semanticScores.isEmpty() ? RetrievalMode.LEXICAL_FALLBACK
                    : lexical.isEmpty() ? RetrievalMode.SEMANTIC : RetrievalMode.HYBRID;
            metrics.counter("rag.retrieval.requests", "mode", mode.name().toLowerCase(Locale.ROOT)).increment();
            return new Result(ranked, mode, embeddings.provider(), embeddings.model(), semanticScores, lexicalScores,
                    finals);
        } catch (RuntimeException ex) {
            log.warn("Semantic retrieval unavailable; using lexical fallback: {}", ex.getClass().getSimpleName());
            metrics.counter("rag.retrieval.fallback", "reason", ex.getClass().getSimpleName()).increment();
            return lexicalFallback(lexical);
        }
    }

    private Result lexicalFallback(List<Product> lexical) {
        Map<Long, Double> scores = rankScores(lexical);
        return new Result(lexical, RetrievalMode.LEXICAL_FALLBACK, null, null, Map.of(), scores, scores);
    }

    private Map<Long, Double> rankScores(List<Product> values) {
        Map<Long, Double> result = new LinkedHashMap<>();
        for (int i = 0; i < values.size(); i++) result.put(values.get(i).getId(), 1d / (i + 1));
        return result;
    }

    private boolean valid(Product product, AiSearchProfile profile) {
        if (!Boolean.TRUE.equals(product.getActive()) || product.getStatus() != ProductStatus.ACTIVE
                || product.getStockQuantity() == null || product.getStockQuantity() <= 0) return false;
        if (profile.minPrice() != null && product.getPrice().compareTo(profile.minPrice()) < 0) return false;
        if (profile.maxPrice() != null && product.getPrice().compareTo(profile.maxPrice()) > 0) return false;
        return profile.languages() == null || profile.languages().isEmpty() || profile.languages().stream()
                .anyMatch(lang -> product.getBookLanguage() != null
                        && product.getBookLanguage().toLowerCase(Locale.ROOT).contains(lang.toLowerCase(Locale.ROOT)));
    }

    private double popularity(Product product) {
        double rating = product.getAverageRating() == null ? 0 : product.getAverageRating().doubleValue() / 5d;
        return (rating + Math.min(1, Math.log1p(Math.max(0, product.getSoldCount())) / 10d)) / 2d;
    }
}
