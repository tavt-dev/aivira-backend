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
public class CartItemResponse {
    Long id;
    Long productId;
    Long productVariationId;
    String productName;
    String productSlug;
    String thumbnailUrl;
    String sku;
    String color;
    String size;
    BigDecimal basePrice;
    BigDecimal additionalPrice;
    BigDecimal finalPrice;
    Integer quantity;
    Integer stockQuantity;
    Boolean available;
}
