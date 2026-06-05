package com.tien.aivirabackend.domain.entity.transaction;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.*;

import com.tien.aivirabackend.constant.RefundStatus;
import com.tien.aivirabackend.domain.entity.BaseEntity;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "refunds")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Refund extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "refund_code", nullable = false, unique = true, length = 50)
    String refundCode;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    Order order;

    @Column(nullable = false, precision = 19, scale = 2)
    BigDecimal amount;

    @Column(nullable = false, length = 255)
    String reason;

    @Column(nullable = false, length = 1000)
    String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    RefundStatus status;

    @Column(name = "refunded_by", nullable = false)
    String refundedBy;

    @Column(name = "refunded_at", nullable = false)
    Instant refundedAt;
}
