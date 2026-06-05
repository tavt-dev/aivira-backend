package com.tien.aivirabackend.domain.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LowStockBookResponse {
    Long productId;
    String productName;
    String slug;
    String sku;
    String thumbnailUrl;
    Integer stockQuantity;
}
