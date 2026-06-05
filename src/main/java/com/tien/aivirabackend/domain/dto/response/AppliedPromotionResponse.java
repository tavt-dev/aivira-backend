package com.tien.aivirabackend.domain.dto.response;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Promotion selected by the discount engine for a checkout item.")
public class AppliedPromotionResponse {
    @Schema(example = "11")
    Long promotionId;
    @Schema(example = "Programming Books Week")
    String promotionName;
    @Schema(example = "50000")
    BigDecimal discountAmount;
}
