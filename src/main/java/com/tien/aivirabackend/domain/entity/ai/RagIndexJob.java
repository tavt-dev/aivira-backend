package com.tien.aivirabackend.domain.entity.ai;

import java.time.Instant;

import jakarta.persistence.*;

import com.tien.aivirabackend.constant.*;
import com.tien.aivirabackend.domain.entity.BaseEntity;

import lombok.*;

@Entity
@Table(name = "rag_index_jobs")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class RagIndexJob extends BaseEntity {
    @Id @Column(length = 36) String id;
    @Enumerated(EnumType.STRING) @Column(name = "job_type", nullable = false, length = 20) RagJobType jobType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) RagJobStatus status;
    @Column(name = "total_items", nullable = false) int totalItems;
    @Column(name = "succeeded_items", nullable = false) int succeededItems;
    @Column(name = "failed_items", nullable = false) int failedItems;
    @Column(name = "error_message", length = 1000) String errorMessage;
    @Column(name = "started_at") Instant startedAt;
    @Column(name = "completed_at") Instant completedAt;
}

