package com.tien.aivirabackend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tien.aivirabackend.domain.entity.ai.AiAdviceRecommendation;

public interface AiAdviceRecommendationRepository extends JpaRepository<AiAdviceRecommendation, Long> {
    @EntityGraph(attributePaths = { "product", "product.category" })
    List<AiAdviceRecommendation> findBySnapshotIdOrderByRankPositionAsc(Long snapshotId, Pageable pageable);

    Optional<AiAdviceRecommendation> findByIdAndSnapshotMessageSessionId(Long id, String sessionId);
}
