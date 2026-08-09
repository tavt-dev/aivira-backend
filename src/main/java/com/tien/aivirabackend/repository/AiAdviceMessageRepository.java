package com.tien.aivirabackend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tien.aivirabackend.domain.entity.ai.AiAdviceMessage;
import com.tien.aivirabackend.constant.AiAdviceRole;

public interface AiAdviceMessageRepository extends JpaRepository<AiAdviceMessage, Long> {
    List<AiAdviceMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    List<AiAdviceMessage> findBySessionIdOrderByCreatedAtDesc(String sessionId, Pageable pageable);

    Optional<AiAdviceMessage> findBySessionIdAndClientMessageId(String sessionId, String clientMessageId);

    Optional<AiAdviceMessage> findByIdAndSessionId(Long id, String sessionId);

    Optional<AiAdviceMessage> findFirstBySessionIdAndRoleAndIdGreaterThanOrderByIdAsc(
            String sessionId, AiAdviceRole role, Long id);
}
