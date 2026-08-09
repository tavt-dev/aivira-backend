package com.tien.aivirabackend.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tien.aivirabackend.domain.entity.ai.AiAdviceSession;

public interface AiAdviceSessionRepository extends JpaRepository<AiAdviceSession, String> {
    Optional<AiAdviceSession> findByIdAndUserId(String id, String userId);

    long deleteByExpiresAtBefore(Instant cutoff);
}
