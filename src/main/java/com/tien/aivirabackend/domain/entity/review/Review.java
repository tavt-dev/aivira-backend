package com.tien.aivirabackend.domain.entity.review;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import com.tien.aivirabackend.domain.entity.BaseEntity;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.entity.catalog.ProductVariation;
import com.tien.aivirabackend.domain.entity.transaction.Order;
import com.tien.aivirabackend.domain.entity.transaction.OrderItem;
import com.tien.aivirabackend.domain.entity.user.User;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Review extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /* CONTENT */
    @Min(1)
    @Max(5)
    @Column(nullable = false)
    Integer rating;

    @Column(columnDefinition = "TEXT")
    String comment;

    /* STATUS */
    @Builder.Default
    @Column(name = "is_approved", nullable = false)
    boolean approved = false;

    @Builder.Default
    @Column(name = "is_visible", nullable = false)
    boolean visible = true;

    @Column(name = "deleted_at")
    Instant deletedAt;

    @Column(name = "admin_reply", columnDefinition = "TEXT")
    String adminReply;

    @Column(name = "moderated_by")
    String moderatedBy;

    @Column(name = "moderated_at")
    Instant moderatedAt;

    @Column(name = "replied_by")
    String repliedBy;

    @Column(name = "replied_at")
    Instant repliedAt;

    /* RELATIONSHIP */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variation_id", nullable = false)
    ProductVariation productVariation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    Order order;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", unique = true)
    OrderItem orderItem;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<ReviewImage> images = new ArrayList<>();
}
