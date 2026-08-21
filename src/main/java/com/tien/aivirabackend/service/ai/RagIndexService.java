package com.tien.aivirabackend.service.ai;

import java.util.*;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import com.tien.aivirabackend.config.properties.RagProperties;
import com.tien.aivirabackend.constant.*;
import com.tien.aivirabackend.domain.dto.response.*;
import com.tien.aivirabackend.domain.entity.ai.*;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.ProductErrorCode;
import com.tien.aivirabackend.exception.errorCode.AiAdviceErrorCode;
import com.tien.aivirabackend.repository.*;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RagIndexService {
    private final ProductRepository products;
    private final RagProductIndexRepository indexes;
    private final RagIndexJobRepository jobs;
    private final RagIndexWorker worker;
    private final EmbeddingClient embeddings;
    private final QdrantVectorStore qdrant;
    private final RagProperties properties;

    public RagIndexJobResponse startFullReindex() {
        if (!properties.enabled()) throw new AppException(AiAdviceErrorCode.AI_ADVISOR_UNAVAILABLE);
        if (jobs.existsByJobTypeAndStatusIn(RagJobType.FULL, List.of(RagJobStatus.PENDING, RagJobStatus.RUNNING)))
            throw new AppException(AiAdviceErrorCode.RAG_JOB_CONFLICT);
        RagIndexJob job = jobs.save(RagIndexJob.builder().id(UUID.randomUUID().toString()).jobType(RagJobType.FULL)
                .status(RagJobStatus.PENDING).build());
        worker.fullReindexAsync(job.getId());
        return response(job);
    }

    public RagIndexJobResponse reindexProduct(Long productId) {
        if (!properties.enabled()) throw new AppException(AiAdviceErrorCode.AI_ADVISOR_UNAVAILABLE);
        if (!products.existsById(productId)) throw new AppException(ProductErrorCode.PRODUCT_NOT_FOUND);
        RagIndexJob job = jobs.save(RagIndexJob.builder().id(UUID.randomUUID().toString()).jobType(RagJobType.PRODUCT)
                .status(RagJobStatus.PENDING).totalItems(1).build());
        worker.productJobAsync(job.getId(), productId);
        return response(job);
    }

    public RagIndexJobResponse getJob(String id) {
        return response(jobs.findById(id)
                .orElseThrow(() -> new AppException(AiAdviceErrorCode.RAG_JOB_NOT_FOUND)));
    }

    public RagIndexStatusResponse status() {
        return new RagIndexStatusResponse(properties.enabled(), properties.enabled() && qdrant.healthy(),
                qdrant.collectionName(embeddings), embeddings.provider(), embeddings.model(), embeddings.dimension(),
                indexes.countByStatus(RagIndexStatus.INDEXED), indexes.countByStatus(RagIndexStatus.PENDING),
                indexes.countByStatus(RagIndexStatus.FAILED));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void resumeInterruptedFullReindex() {
        if (!properties.enabled() || !embeddings.configured()) return;
        jobs.findByJobTypeAndStatusInOrderByCreatedAtAsc(RagJobType.FULL,
                List.of(RagJobStatus.PENDING, RagJobStatus.RUNNING)).stream().findFirst()
                .ifPresent(job -> worker.fullReindexAsync(job.getId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void catalogChanged(ProductCatalogChangedEvent event) { worker.indexProductAsync(event.productId()); }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void categoryChanged(CategoryCatalogChangedEvent event) {
        products.findByCategoryId(event.categoryId()).stream().map(Product::getId).forEach(worker::indexProductAsync);
    }

    @Scheduled(cron = "${rag.reconcile-cron:0 */15 * * * *}")
    public void reconcile() {
        if (!properties.enabled() || !embeddings.configured()) return;
        Map<Long, RagProductIndex> states = indexes.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(RagProductIndex::getProductId, row -> row));
        products.findAll().stream().map(Product::getId).filter(id -> {
            RagProductIndex state = states.get(id);
            return state == null || state.getStatus() != RagIndexStatus.FAILED
                    || state.getRetryCount() < properties.maxRetries();
        }).forEach(worker::indexProductAsync);
        indexes.findByStatusInOrderByUpdatedAtAsc(List.of(RagIndexStatus.PENDING, RagIndexStatus.FAILED),
                PageRequest.of(0, properties.indexBatchSize())).stream()
                .filter(row -> row.getRetryCount() < properties.maxRetries()).map(RagProductIndex::getProductId)
                .forEach(worker::indexProductAsync);
    }

    private RagIndexJobResponse response(RagIndexJob job) {
        return new RagIndexJobResponse(job.getId(), job.getJobType(), job.getStatus(), job.getTotalItems(),
                job.getSucceededItems(), job.getFailedItems(), job.getErrorMessage(), job.getStartedAt(),
                job.getCompletedAt());
    }
}
