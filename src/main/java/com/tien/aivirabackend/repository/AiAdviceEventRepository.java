package com.tien.aivirabackend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tien.aivirabackend.constant.AiAdviceEventType;
import com.tien.aivirabackend.domain.entity.ai.AiAdviceEvent;

public interface AiAdviceEventRepository extends JpaRepository<AiAdviceEvent, Long> {
    Optional<AiAdviceEvent> findBySessionIdAndMessageIdAndEventTypeIn(
            String sessionId, Long messageId, Iterable<AiAdviceEventType> eventTypes);

    boolean existsBySessionIdAndRecommendationIdAndEventType(
            String sessionId, Long recommendationId, AiAdviceEventType eventType);
}
