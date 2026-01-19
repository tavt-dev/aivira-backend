package com.tien.aivirabackend.domain.entity.discount;

import com.tien.aivirabackend.constant.PromotionScope;
import com.tien.aivirabackend.constant.PromotionType;
import com.tien.aivirabackend.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Promotion extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /* BASIC INFO */
    @Column(nullable = false, unique = true, length = 150, name = "promotion_name")
    String promotionName;

    @Lob
    @Column(nullable = false)
    String description;

    /* DISCOUNT RULE */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "promotion_type")
    PromotionType promotionType;

    @Column(nullable = false, precision = 19, scale = 2)
    BigDecimal value;

    @Column(name = "max_discount_amount", precision = 19, scale = 2)
    BigDecimal maxDiscountAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "promotion_scope", nullable = false)
    PromotionScope promotionScope;

    @Column(name = "target_id", nullable = false)
    Long targetId;

    /* TIME WINDOW */
    @Column(name = "start_at", nullable = false)
    LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    LocalDateTime endAt;

    /* STATUS */
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    Boolean active = true;
}
