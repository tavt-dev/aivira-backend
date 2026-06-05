package com.tien.aivirabackend.domain.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

import com.tien.aivirabackend.constant.CouponType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Coupon response with usage counters. Used count increases only when usage is finalized.")
public class CouponResponse {
    @Schema(example = "7")
    Long id;
    @Schema(example = "AIVIRA10")
    String code;
    @Schema(example = "PERCENT")
    CouponType type;
    @Schema(example = "10")
    BigDecimal value;
    @Schema(example = "50000")
    BigDecimal maxDiscountAmount;
    @Schema(example = "300000")
    BigDecimal minOrderAmount;
    Integer usageLimit;
    Integer usageLimitPerUser;
    @Schema(example = "12")
    Integer usedCount;
    LocalDateTime startAt;
    LocalDateTime endAt;
    Boolean active;
    Instant createdAt;
    Instant updatedAt;
}
