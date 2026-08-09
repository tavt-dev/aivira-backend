package com.tien.aivirabackend.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tien.aivirabackend.constant.AiAdviceUsageStatus;
import com.tien.aivirabackend.domain.entity.ai.AiAdviceUsage;

public interface AiAdviceUsageRepository extends JpaRepository<AiAdviceUsage, Long> {
    Optional<AiAdviceUsage> findByUserIdAndPeriodKeyAndClientMessageId(
            String userId, String periodKey, String clientMessageId);

    List<AiAdviceUsage> findByStatusAndUpdatedAtBefore(AiAdviceUsageStatus status, Instant cutoff);
}
