package com.tien.aivirabackend.domain.entity.notification;

import java.time.Instant;

import jakarta.persistence.*;

import com.tien.aivirabackend.constant.NotificationResourceType;
import com.tien.aivirabackend.constant.NotificationType;
import com.tien.aivirabackend.domain.entity.BaseEntity;
import com.tien.aivirabackend.domain.entity.user.User;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Notification extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    NotificationType type;

    @Column(nullable = false, length = 255)
    String title;

    @Column(nullable = false, length = 1000)
    String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", length = 50)
    NotificationResourceType resourceType;

    @Column(name = "resource_id", length = 100)
    String resourceId;

    @Column(name = "action_url", length = 500)
    String actionUrl;

    @Column(columnDefinition = "json")
    String payload;

    @Column(name = "read_at")
    Instant readAt;
}
