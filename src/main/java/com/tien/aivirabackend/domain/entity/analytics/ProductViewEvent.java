package com.tien.aivirabackend.domain.entity.analytics;

import java.time.Instant;

import jakarta.persistence.*;

import com.tien.aivirabackend.constant.ProductViewSource;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.entity.user.User;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "product_view_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductViewEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    User user;

    @Column(name = "anonymous_id_hash", length = 64)
    String anonymousIdHash;

    @Column(name = "session_id_hash", length = 64)
    String sessionIdHash;

    @Column(name = "viewer_key", nullable = false, length = 64)
    String viewerKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    ProductViewSource source;

    @Column(name = "referrer_path", length = 500)
    String referrerPath;

    @Column(name = "viewed_at", nullable = false)
    Instant viewedAt;

    @Column(name = "deduplication_bucket", nullable = false)
    Long deduplicationBucket;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
