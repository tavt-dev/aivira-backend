package com.tien.aivirabackend.domain.entity.transaction.payment;

import jakarta.persistence.*;

import com.tien.aivirabackend.constant.PaymentProvider;
import com.tien.aivirabackend.domain.entity.BaseEntity;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(
        name = "payment_callbacks",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_payment_callbacks_provider_event",
                        columnNames = {"provider", "event_key"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentCallback extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    PaymentProvider provider;

    @Column(name = "event_key", nullable = false, length = 150)
    String eventKey;

    @Column(name = "payment_code", length = 50)
    String paymentCode;

    @Column(nullable = false, length = 30)
    String status;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    String rawPayload;
}
