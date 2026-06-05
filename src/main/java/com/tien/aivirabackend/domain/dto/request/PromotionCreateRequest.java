package com.tien.aivirabackend.domain.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Admin request to create a product-scope or category-scope bookstore promotion.")
public class PromotionCreateRequest {
    @Schema(example = "Programming Books Week")
    @NotBlank
    @Size(max = 150)
    String promotionName;

    @Schema(example = "Discount for selected programming books.")
    @NotBlank
    String description;

    @Schema(example = "PERCENT")
    @NotNull
    PromotionType promotionType;

    @Schema(description = "Percent value or fixed amount depending on promotionType.", example = "15")
    @NotNull
    BigDecimal value;

    @Schema(example = "75000")
    BigDecimal maxDiscountAmount;

    @Schema(example = "CATEGORY")
    @NotNull
    PromotionScope promotionScope;

    @Schema(description = "Product id for PRODUCT scope or category id for CATEGORY scope.", example = "12")
    @NotNull
    Long targetId;

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
