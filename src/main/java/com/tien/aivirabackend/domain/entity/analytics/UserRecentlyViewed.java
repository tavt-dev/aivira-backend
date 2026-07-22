package com.tien.aivirabackend.domain.entity.analytics;

import java.time.Instant;

import jakarta.persistence.*;

import com.tien.aivirabackend.constant.ProductViewSource;
import com.tien.aivirabackend.domain.entity.BaseEntity;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.entity.user.User;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "user_recently_viewed")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserRecentlyViewed extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    Product product;

    @Column(name = "first_viewed_at", nullable = false)
    Instant firstViewedAt;

    @Column(name = "last_viewed_at", nullable = false)
    Instant lastViewedAt;

    @Column(name = "view_count", nullable = false)
    Long viewCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_source", nullable = false, length = 30)
    ProductViewSource lastSource;
}
