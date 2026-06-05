package com.tien.aivirabackend.domain.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.Size;

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
@Schema(description = "Partial admin coupon update request. Null fields are left unchanged.")
public class CouponUpdateRequest {
    @Schema(description = "Coupon code is normalized to uppercase when updated.", example = "AIVIRA10")
    @Size(max = 50)
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
    @Schema(example = "2026-06-01T00:00:00")
    LocalDateTime startAt;
    @Schema(example = "2026-06-30T23:59:59")
    LocalDateTime endAt;
    @Schema(example = "true")
    Boolean active;
}
