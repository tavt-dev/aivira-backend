package com.tien.aivirabackend.domain.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
public class PromotionUpdateRequest {
    @Size(max = 150)
    String promotionName;

    String description;
    PromotionType promotionType;
    BigDecimal value;
    BigDecimal maxDiscountAmount;
    PromotionScope promotionScope;
    Long targetId;
    LocalDateTime startAt;
    LocalDateTime endAt;
    Boolean active;
}
