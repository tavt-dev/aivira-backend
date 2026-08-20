package com.tien.aivirabackend.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.tien.aivirabackend.domain.entity.ai.AiAdviceSession;

public interface AiAdviceSessionRepository extends JpaRepository<AiAdviceSession, String> {
    @EntityGraph(attributePaths = "user")
    Optional<AiAdviceSession> findByIdAndUserId(String id, String userId);

    Optional<AiAdviceSession> findByIdAndGuestKey(String id, String guestKey);

    long deleteByExpiresAtBefore(Instant cutoff);
}
