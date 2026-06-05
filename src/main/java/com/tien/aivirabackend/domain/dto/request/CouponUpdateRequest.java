package com.tien.aivirabackend.domain.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.Size;

import com.tien.aivirabackend.constant.CouponType;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CouponUpdateRequest {
    @Size(max = 50)
    String code;

    CouponType type;
    BigDecimal value;
    BigDecimal maxDiscountAmount;
    BigDecimal minOrderAmount;
    Integer usageLimit;
    Integer usageLimitPerUser;
    LocalDateTime startAt;
    LocalDateTime endAt;
    Boolean active;
}
