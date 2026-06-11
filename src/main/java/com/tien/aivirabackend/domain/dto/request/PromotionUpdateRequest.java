package com.tien.aivirabackend.domain.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.Size;

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
@Schema(description = "Partial admin promotion update request. Null fields are left unchanged.")
public class PromotionUpdateRequest {
    @Schema(example = "Programming Books Week")
    @Size(max = 150)
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

    @Schema(description = "Product id for PRODUCT scope or category id for CATEGORY scope.", example = "12")
    Long targetId;

    LocalDateTime startAt;
    LocalDateTime endAt;

    @Schema(example = "true")
    Boolean active;
}
