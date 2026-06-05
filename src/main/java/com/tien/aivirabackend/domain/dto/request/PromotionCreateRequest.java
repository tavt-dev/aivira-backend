package com.tien.aivirabackend.domain.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.tien.aivirabackend.constant.PromotionScope;
import com.tien.aivirabackend.constant.PromotionType;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromotionCreateRequest {
    @NotBlank
    @Size(max = 150)
    String promotionName;

    @NotBlank
    String description;

    @NotNull
    PromotionType promotionType;

    @NotNull
    BigDecimal value;

    BigDecimal maxDiscountAmount;

    @NotNull
    PromotionScope promotionScope;

    @NotNull
    Long targetId;

    @NotNull
    LocalDateTime startAt;

    @NotNull
    LocalDateTime endAt;

    @Builder.Default
    Boolean active = true;
}
