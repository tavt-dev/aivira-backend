package com.tien.aivirabackend.domain.entity.ai;

import java.time.Instant;

import jakarta.persistence.*;

import com.tien.aivirabackend.constant.RagIndexStatus;
import com.tien.aivirabackend.domain.entity.BaseEntity;
import com.tien.aivirabackend.domain.entity.catalog.Product;

import lombok.*;

@Entity
@Table(name = "rag_product_index")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class RagProductIndex extends BaseEntity {
    @Id @Column(name = "product_id") Long productId;
    @MapsId @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "product_id") Product product;
    @Column(name = "content_hash", length = 64) String contentHash;
    @Column(length = 30) String provider;
    @Column(length = 100) String model;
    Integer dimension;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) RagIndexStatus status;
    @Column(name = "retry_count", nullable = false) int retryCount;
    @Column(name = "last_error", length = 1000) String lastError;
    @Column(name = "indexed_at") Instant indexedAt;
}

