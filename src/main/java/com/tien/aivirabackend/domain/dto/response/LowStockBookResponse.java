package com.tien.aivirabackend.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Low-stock active book row for admin dashboard.")
public class LowStockBookResponse {
    @Schema(example = "101")
    Long productId;
    @Schema(example = "Clean Code")
    String productName;
    @Schema(example = "clean-code")
    String slug;
    @Schema(example = "BOOK-CLN-CODE-PB")
    String sku;
    String thumbnailUrl;
    @Schema(example = "3")
    Integer stockQuantity;
}
