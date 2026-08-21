package com.tien.aivirabackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tien.aivirabackend.constant.RagJobStatus;
import com.tien.aivirabackend.constant.RagJobType;
import com.tien.aivirabackend.domain.entity.ai.RagIndexJob;

import java.util.List;

public interface RagIndexJobRepository extends JpaRepository<RagIndexJob, String> {
    boolean existsByJobTypeAndStatusIn(com.tien.aivirabackend.constant.RagJobType type, Iterable<RagJobStatus> statuses);

    List<RagIndexJob> findByJobTypeAndStatusInOrderByCreatedAtAsc(RagJobType type, Iterable<RagJobStatus> statuses);
}
