package com.tien.aivirabackend.domain.entity.notification;

import java.time.Instant;

import jakarta.persistence.*;

import com.tien.aivirabackend.constant.NotificationChannel;
import com.tien.aivirabackend.constant.NotificationOutboxStatus;
import com.tien.aivirabackend.domain.entity.BaseEntity;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "notification_outbox")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationOutbox extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "event_key", nullable = false, unique = true, length = 200)
    String eventKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    Notification notification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    NotificationOutboxStatus status;

    @Column(name = "attempt_count", nullable = false)
    int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    Instant nextAttemptAt;

    @Column(name = "locked_at")
    Instant lockedAt;

    @Column(name = "processed_at")
    Instant processedAt;

    @Column(name = "last_error", length = 1000)
    String lastError;
}
