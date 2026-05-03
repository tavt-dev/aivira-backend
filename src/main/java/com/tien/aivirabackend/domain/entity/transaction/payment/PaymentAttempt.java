package com.tien.aivirabackend.domain.entity.transaction.payment;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.*;

import com.tien.aivirabackend.constant.PaymentMethod;
import com.tien.aivirabackend.constant.PaymentProvider;
import com.tien.aivirabackend.constant.PaymentStatus;
import com.tien.aivirabackend.domain.entity.BaseEntity;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(
        name = "payment_attempts",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_payment_attempts_group_attempt",
                        columnNames = {"payment_group_id", "attempt_no"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentAttempt extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_group_id", nullable = false)
    PaymentGroup paymentGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    PaymentMethod method;

    @Column(name = "attempt_no", nullable = false)
    Integer attemptNo;

    @Column(name = "provider_txn_ref", length = 100)
    String providerTxnRef;

    @Column(name = "request_id", length = 100)
    String requestId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false, precision = 19, scale = 2)
    BigDecimal amount;

    @Column(name = "payment_url", length = 2000)
    String paymentUrl;

    @Column(length = 2000)
    String deeplink;

    @Column(name = "qr_code_url", length = 2000)
    String qrCodeUrl;

    @Column(name = "provider_transaction_id", length = 100)
    String providerTransactionId;

    @Column(name = "raw_request", columnDefinition = "TEXT")
    String rawRequest;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    String rawResponse;

    @Column(name = "expires_at")
    Instant expiresAt;

    @Column(name = "completed_at")
    Instant completedAt;
}
