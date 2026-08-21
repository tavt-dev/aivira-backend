package com.tien.aivirabackend.service.ai;

import java.time.Instant;
import java.util.*;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;

import com.tien.aivirabackend.config.properties.RagProperties;
import com.tien.aivirabackend.constant.*;
import com.tien.aivirabackend.domain.entity.ai.*;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.micrometer.core.instrument.MeterRegistry;

@Component
@RequiredArgsConstructor
@Slf4j
public class RagIndexWorker {
    private final ProductRepository products;
    private final RagProductIndexRepository indexRepository;
    private final RagIndexJobRepository jobs;
    private final EmbeddingClient embeddings;
    private final QdrantVectorStore qdrant;
    private final RagProperties properties;
    private final MeterRegistry metrics;

    @Async("ragTaskExecutor")
    @Transactional
    public void indexProductAsync(Long productId) { if (properties.enabled()) indexProduct(productId); }

    @Async("ragTaskExecutor")
    @Transactional
    public void fullReindexAsync(String jobId) {
        RagIndexJob job = jobs.findById(jobId).orElseThrow();
        job.setStatus(RagJobStatus.RUNNING); job.setStartedAt(Instant.now());
        List<Long> ids = products.findAll().stream().map(Product::getId).toList();
        job.setTotalItems(ids.size()); jobs.saveAndFlush(job);
        for (Long id : ids) {
            try { indexProduct(id); job.setSucceededItems(job.getSucceededItems() + 1); }
            catch (RuntimeException ex) { job.setFailedItems(job.getFailedItems() + 1); job.setErrorMessage(shortError(ex)); }
        }
        job.setStatus(job.getFailedItems() == 0 ? RagJobStatus.COMPLETED : RagJobStatus.PARTIAL);
        if (job.getFailedItems() == 0) qdrant.activateAlias(embeddings);
        job.setCompletedAt(Instant.now()); jobs.save(job);
    }

    @Async("ragTaskExecutor")
    @Transactional
    public void productJobAsync(String jobId, Long productId) {
        RagIndexJob job = jobs.findById(jobId).orElseThrow();
        job.setStatus(RagJobStatus.RUNNING); job.setStartedAt(Instant.now());
        try {
            indexProduct(productId); job.setSucceededItems(1); job.setStatus(RagJobStatus.COMPLETED);
        } catch (RuntimeException ex) {
            job.setFailedItems(1); job.setErrorMessage(shortError(ex)); job.setStatus(RagJobStatus.FAILED);
        }
        job.setCompletedAt(Instant.now()); jobs.save(job);
    }

    public void indexProduct(Long productId) {
        Optional<Product> found = products.findDetailedById(productId);
        if (found.isEmpty()) return;
        Product product = found.get();
        RagProductIndex state = indexRepository.findById(productId).orElseGet(() -> RagProductIndex.builder()
                // productId is derived from the associated Product by @MapsId. Leaving it null
                // also lets Spring Data recognize this as a new entity and call persist(), not merge().
                .product(product).status(RagIndexStatus.PENDING).build());
        try {
            if (!Boolean.TRUE.equals(product.getActive()) || product.getStatus() != ProductStatus.ACTIVE) {
                qdrant.delete(embeddings, productId); state.setStatus(RagIndexStatus.DELETED);
            } else {
                RagBookDocument document = RagBookDocument.from(product, embeddings.provider(), embeddings.model());
                boolean unchanged = document.contentHash().equals(state.getContentHash())
                        && embeddings.provider().equals(state.getProvider()) && embeddings.model().equals(state.getModel())
                        && Objects.equals(embeddings.dimension(), state.getDimension()) && qdrant.pointExists(embeddings, productId);
                if (unchanged) qdrant.updatePayload(embeddings, productId, document.payload());
                else qdrant.upsert(embeddings, productId, embeddings.embedDocument(document.title(), document.content()),
                        document.payload());
                state.setContentHash(document.contentHash()); state.setProvider(embeddings.provider());
                state.setModel(embeddings.model()); state.setDimension(embeddings.dimension());
                state.setStatus(RagIndexStatus.INDEXED); state.setRetryCount(0); state.setLastError(null);
                state.setIndexedAt(Instant.now());
            }
            metrics.counter("rag.index.operations", "status", "success").increment();
        } catch (RuntimeException ex) {
            int attempts = state.getRetryCount() + 1;
            state.setRetryCount(retryable(ex) ? attempts : properties.maxRetries());
            state.setStatus(RagIndexStatus.FAILED);
            state.setLastError(shortError(ex)); indexRepository.save(state);
            metrics.counter("rag.index.operations", "status", "failure").increment();
            throw ex;
        }
        indexRepository.save(state);
    }

    private String shortError(Throwable ex) {
        String value = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        return value.substring(0, Math.min(1000, value.length()));
    }

    private boolean retryable(RuntimeException ex) {
        if (!(ex instanceof RestClientResponseException response)) return true;
        int status = response.getStatusCode().value();
        return status == 429 || status >= 500;
    }
}
