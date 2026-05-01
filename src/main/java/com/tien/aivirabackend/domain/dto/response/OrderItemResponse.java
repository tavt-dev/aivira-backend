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
public class OrderItemResponse {
    Long id;
    Long productId;
    Long productVariationId;
    String productName;
    String sku;
    String variationColor;
    String variationSize;
    String thumbnailUrl;
    BigDecimal basePrice;
    BigDecimal additionalPrice;
    BigDecimal discountAmount;
    BigDecimal finalPrice;
    Integer quantity;
}
