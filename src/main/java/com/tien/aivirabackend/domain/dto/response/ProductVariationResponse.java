package com.tien.aivirabackend.domain.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductVariationResponse {
    Long id;
    String sku;
    String color;
    String size;
    BigDecimal additionalPrice;
    Integer stockQuantity;
    String imageUrl;
    String imagePublicId;
    Boolean active;
    Instant createdAt;
    Instant updatedAt;
}
