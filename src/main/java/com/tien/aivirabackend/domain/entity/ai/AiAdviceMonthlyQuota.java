package com.tien.aivirabackend.domain.entity.ai;

import jakarta.persistence.*;

import com.tien.aivirabackend.domain.entity.BaseEntity;
import com.tien.aivirabackend.domain.entity.user.User;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "ai_advice_monthly_quotas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiAdviceMonthlyQuota extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(name = "period_key", nullable = false, length = 7)
    String periodKey;

    @Column(name = "used_count", nullable = false)
    int usedCount;

    @Column(name = "reserved_count", nullable = false)
    int reservedCount;
}
