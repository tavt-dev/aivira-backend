package com.tien.aivirabackend.domain.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Book variation response, commonly paperback, hardcover, ebook, or a default single variant.")
public class ProductVariationResponse {
    Long id;
    @Schema(example = "BOOK-CLN-CODE-PB")
    String sku;
    @Schema(example = "Default")
    String color;
    @Schema(example = "Paperback")
    String size;
    @Schema(example = "0")
    BigDecimal additionalPrice;
    @Schema(example = "20")
    Integer stockQuantity;
    String imageUrl;
    String imagePublicId;
    Boolean active;
    Instant createdAt;
    Instant updatedAt;
}
