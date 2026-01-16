package com.tien.aivirabackend.domain.entity.discount;

import com.tien.aivirabackend.constant.CouponType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true, length = 50)
    String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    CouponType type;

    @Column(nullable = false, precision = 19, scale = 2)
    BigDecimal value;

    @Column(name = "max_discount_amount", precision = 19, scale = 2)
    BigDecimal maxDiscountAmount;

    @Column(name = "min_order_amount", precision = 19, scale = 2)
    BigDecimal minOrderAmount;

    @Column(name = "usage_limit")
    Integer usageLimit; // tổng số lượt dùng

    @Column(name = "usage_limit_per_user")
    Integer usageLimitPerUser; // số lượt dùng tối đa cho mỗi user

    @Column(name = "used_count", nullable = false)
    @Builder.Default
    Integer usedCount = 0;

    @Column(name = "start_at", nullable = false)
    LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    LocalDateTime endAt;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    Boolean active = true;

    @OneToMany(mappedBy = "coupon", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<CouponUsage> usages = new ArrayList<>();
}
