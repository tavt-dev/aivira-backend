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
public class CheckoutPreviewItemResponse {
    Long cartItemId;
    Long productId;
    Long productVariationId;
    String productName;
    String sku;
    Integer quantity;
    BigDecimal unitPrice;
    BigDecimal lineSubtotal;
    BigDecimal promotionDiscountAmount;
    String promotionName;
    BigDecimal finalLineAmount;
}
