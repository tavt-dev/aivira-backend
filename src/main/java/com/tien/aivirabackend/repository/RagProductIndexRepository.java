package com.tien.aivirabackend.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tien.aivirabackend.constant.RagIndexStatus;
import com.tien.aivirabackend.domain.entity.ai.RagProductIndex;

public interface RagProductIndexRepository extends JpaRepository<RagProductIndex, Long> {
    List<RagProductIndex> findByStatusInOrderByUpdatedAtAsc(List<RagIndexStatus> statuses, Pageable pageable);
    long countByStatus(RagIndexStatus status);
}

