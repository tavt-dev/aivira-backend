package com.tien.aivirabackend.domain.entity.ai;

import jakarta.persistence.*;

import com.tien.aivirabackend.constant.AiAdviceUsageStatus;
import com.tien.aivirabackend.domain.entity.BaseEntity;
import com.tien.aivirabackend.domain.entity.user.User;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "ai_advice_usages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiAdviceUsage extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(name = "period_key", nullable = false, length = 7)
    String periodKey;

    @Column(name = "client_message_id", nullable = false, length = 36)
    String clientMessageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    AiAdviceUsageStatus status;
}
