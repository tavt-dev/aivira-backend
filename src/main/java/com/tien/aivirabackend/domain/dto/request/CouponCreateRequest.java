package com.tien.aivirabackend.domain.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Admin request to create an order-level coupon. Promotions apply first; coupons apply after promotions.")
public class CouponCreateRequest {
    @Schema(description = "Coupon code is normalized to uppercase.", example = "AIVIRA10")
    @NotBlank
    @Size(max = 50)
    String code;

    @Schema(example = "PERCENT")
    @NotNull
    CouponType type;

    @Schema(description = "Percent value or fixed amount depending on type.", example = "10")
    @NotNull
    BigDecimal value;

    @Schema(example = "50000")
    BigDecimal maxDiscountAmount;

    @Schema(example = "300000")
    BigDecimal minOrderAmount;

    @Schema(example = "100")
    Integer usageLimit;

    @Schema(example = "1")
    Integer usageLimitPerUser;

    @Schema(example = "2026-06-01T00:00:00")
    @NotNull
    LocalDateTime startAt;

    @Schema(example = "2026-06-30T23:59:59")
    @NotNull
    LocalDateTime endAt;

    @Schema(example = "true")
    @Builder.Default
    Boolean active = true;
}
