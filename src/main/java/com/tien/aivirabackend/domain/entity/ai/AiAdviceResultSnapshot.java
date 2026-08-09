package com.tien.aivirabackend.domain.entity.ai;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

import com.tien.aivirabackend.domain.entity.BaseEntity;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "ai_advice_result_snapshots")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiAdviceResultSnapshot extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false, unique = true)
    AiAdviceMessage message;

    @Column(name = "search_profile", nullable = false, columnDefinition = "LONGTEXT")
    String searchProfile;

    @Column(name = "total_results", nullable = false)
    int totalResults;

    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<AiAdviceRecommendation> recommendations = new ArrayList<>();
}
