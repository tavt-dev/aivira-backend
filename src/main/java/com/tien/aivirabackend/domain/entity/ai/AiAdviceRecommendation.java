package com.tien.aivirabackend.domain.entity.ai;

import jakarta.persistence.*;

import com.tien.aivirabackend.domain.entity.BaseEntity;
import com.tien.aivirabackend.domain.entity.catalog.Product;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "ai_advice_recommendations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiAdviceRecommendation extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "snapshot_id", nullable = false)
    AiAdviceResultSnapshot snapshot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    Product product;

    @Column(name = "rank_position", nullable = false)
    int rankPosition;

    @Column(columnDefinition = "TEXT")
    String reason;

    @Column(name = "matched_criteria", columnDefinition = "TEXT")
    String matchedCriteria;
}
