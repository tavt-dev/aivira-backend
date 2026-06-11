package com.tien.aivirabackend.domain.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

import com.tien.aivirabackend.constant.PromotionScope;
import com.tien.aivirabackend.constant.PromotionType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Promotion response. Promotions apply to products or categories before coupons.")
public class PromotionResponse {
    @Schema(example = "11")
    Long id;

    @Schema(example = "Programming Books Week")
    String promotionName;

    String description;

    @Schema(example = "PERCENT")
    PromotionType promotionType;

    @Schema(example = "15")
    BigDecimal value;

    @Schema(example = "75000")
    BigDecimal maxDiscountAmount;

    @Schema(example = "CATEGORY")
    PromotionScope promotionScope;

    @Schema(example = "12")
    Long targetId;

    LocalDateTime startAt;
    LocalDateTime endAt;
    Boolean active;
    Instant createdAt;
    Instant updatedAt;
}
