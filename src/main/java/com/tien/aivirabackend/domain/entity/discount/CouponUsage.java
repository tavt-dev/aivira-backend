package com.tien.aivirabackend.domain.entity.discount;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;

import com.tien.aivirabackend.constant.CouponUsageStatus;
import com.tien.aivirabackend.domain.entity.BaseEntity;
import com.tien.aivirabackend.domain.entity.transaction.Order;
import com.tien.aivirabackend.domain.entity.user.User;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "coupon_usages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CouponUsage extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "discount_amount", precision = 19, scale = 2, nullable = false)
    BigDecimal discountAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    Order order;

    @Column(name = "used_at")
    LocalDateTime usedAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 30)
    CouponUsageStatus status = CouponUsageStatus.FINALIZED;

    @Column(name = "reserved_at")
    LocalDateTime reservedAt;

    @Column(name = "finalized_at")
    LocalDateTime finalizedAt;

    @Column(name = "released_at")
    LocalDateTime releasedAt;
}
