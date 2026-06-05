package com.tien.aivirabackend.domain.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CouponCreateRequest {
    @NotBlank
    @Size(max = 50)
    String code;

    @NotNull
    CouponType type;

    @NotNull
    BigDecimal value;

    BigDecimal maxDiscountAmount;
    BigDecimal minOrderAmount;
    Integer usageLimit;
    Integer usageLimitPerUser;

    @NotNull
    LocalDateTime startAt;

    @NotNull
    LocalDateTime endAt;

    @Builder.Default
    Boolean active = true;
}
