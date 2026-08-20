package com.tien.aivirabackend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tien.aivirabackend.domain.entity.ai.AiAdviceResultSnapshot;

public interface AiAdviceSnapshotRepository extends JpaRepository<AiAdviceResultSnapshot, Long> {
    Optional<AiAdviceResultSnapshot> findByMessageIdAndMessageSessionId(Long messageId, String sessionId);
}
