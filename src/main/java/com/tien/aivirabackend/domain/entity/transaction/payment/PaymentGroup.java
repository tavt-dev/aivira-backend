package com.tien.aivirabackend.domain.entity.transaction.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

import com.tien.aivirabackend.constant.PaymentMethod;
import com.tien.aivirabackend.constant.PaymentStatus;
import com.tien.aivirabackend.domain.entity.BaseEntity;
import com.tien.aivirabackend.domain.entity.user.User;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "payment_groups")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentGroup extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "payment_code", nullable = false, unique = true, length = 50)
    String paymentCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false, precision = 19, scale = 2)
    BigDecimal amount;

    @Column(name = "provider_txn_ref", length = 100)
    String providerTxnRef;

    @Column(name = "provider_transaction_id", length = 100)
    String providerTransactionId;

    @Column(name = "payment_url", length = 2000)
    String paymentUrl;

    @Column(length = 2000)
    String deeplink;

    @Column(name = "qr_code_url", length = 2000)
    String qrCodeUrl;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    String rawResponse;

    @Column(name = "expires_at")
    Instant expiresAt;

    @Column(name = "paid_at")
    Instant paidAt;

    @OneToMany(mappedBy = "paymentGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<Payment> payments = new ArrayList<>();
}
