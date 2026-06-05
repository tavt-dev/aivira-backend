package com.tien.aivirabackend.domain.dto.response;

import java.math.BigDecimal;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppliedPromotionResponse {
    Long promotionId;
    String promotionName;
    BigDecimal discountAmount;
}
