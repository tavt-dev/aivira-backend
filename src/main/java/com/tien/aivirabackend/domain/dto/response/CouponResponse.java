package com.tien.aivirabackend.domain.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

import com.tien.aivirabackend.constant.CouponType;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CouponResponse {
    Long id;
    String code;
    CouponType type;
    BigDecimal value;
    BigDecimal maxDiscountAmount;
    BigDecimal minOrderAmount;
    Integer usageLimit;
    Integer usageLimitPerUser;
    Integer usedCount;
    LocalDateTime startAt;
    LocalDateTime endAt;
    Boolean active;
    Instant createdAt;
    Instant updatedAt;
}
