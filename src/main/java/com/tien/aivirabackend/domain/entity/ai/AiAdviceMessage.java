package com.tien.aivirabackend.domain.entity.ai;

import jakarta.persistence.*;

import com.tien.aivirabackend.constant.AiAdviceResponseStatus;
import com.tien.aivirabackend.constant.AiAdviceRole;
import com.tien.aivirabackend.constant.RetrievalMode;
import com.tien.aivirabackend.domain.entity.BaseEntity;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "ai_advice_messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiAdviceMessage extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    AiAdviceSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    AiAdviceRole role;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    String content;

    @Column(name = "client_message_id", length = 36)
    String clientMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "response_status", length = 30)
    AiAdviceResponseStatus responseStatus;

    @Column(length = 100)
    String model;

    @Column(length = 30)
    String provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "retrieval_mode", length = 30)
    RetrievalMode retrievalMode;

    @Column(name = "embedding_provider", length = 30)
    String embeddingProvider;

    @Column(name = "embedding_model", length = 100)
    String embeddingModel;

    @Column(name = "input_tokens")
    Integer inputTokens;

    @Column(name = "output_tokens")
    Integer outputTokens;

    @Column(name = "latency_ms")
    Long latencyMs;

    @Column(name = "error_code", length = 80)
    String errorCode;

    @Column(name = "suggested_prompts", columnDefinition = "TEXT")
    String suggestedPrompts;

    @OneToOne(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    AiAdviceResultSnapshot snapshot;
}
