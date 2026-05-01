package com.tien.aivirabackend.domain.entity.transaction.payment;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.*;

import com.tien.aivirabackend.constant.PaymentMethod;
import com.tien.aivirabackend.constant.PaymentStatus;
import com.tien.aivirabackend.domain.entity.BaseEntity;
import com.tien.aivirabackend.domain.entity.transaction.Order;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "payments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Payment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_group_id", nullable = false)
    PaymentGroup paymentGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    PaymentStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    BigDecimal amount;

    @Column(name = "transaction_id", length = 100)
    String transactionId;

    @Column(name = "provider_response", columnDefinition = "TEXT")
    String providerResponse;

    @Column(name = "paid_at")
    Instant paidAt;
}
