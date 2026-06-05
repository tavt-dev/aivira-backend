package com.tien.aivirabackend.domain.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

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
public class PromotionResponse {
    Long id;
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
    Instant createdAt;
    Instant updatedAt;
}
