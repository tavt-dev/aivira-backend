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
@Schema(description = "One cart item line in a non-mutating checkout preview.")
public class CheckoutPreviewItemResponse {
    Long cartItemId;
    Long productId;
    Long productVariationId;
    @Schema(example = "Clean Code")
    String productName;
    @Schema(example = "BOOK-CLN-CODE-PB")
    String sku;
    @Schema(example = "2")
    Integer quantity;
    @Schema(example = "320000")
    BigDecimal unitPrice;
    @Schema(example = "640000")
    BigDecimal lineSubtotal;
    @Schema(example = "50000")
    BigDecimal promotionDiscountAmount;
    String promotionName;
    @Schema(example = "590000")
    BigDecimal finalLineAmount;
}
