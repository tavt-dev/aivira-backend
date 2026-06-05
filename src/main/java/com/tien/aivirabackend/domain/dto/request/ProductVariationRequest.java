package com.tien.aivirabackend.domain.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Book variation request, for example paperback, hardcover, or a default single variant.")
public class ProductVariationRequest {
    @Schema(example = "BOOK-CLN-CODE-PB")
    @NotBlank
    @Size(max = 50)
    String sku;

    @Schema(description = "Kept for compatibility; use Default for books without color variants.", example = "Default")
    @NotBlank
    @Size(max = 50)
    String color;

    @Schema(description = "Book format label such as Paperback, Hardcover, or Ebook.", example = "Paperback")
    @NotBlank
    @Size(max = 50)
    String size;

    @Schema(example = "0")
    @NotNull
    @DecimalMin("0.00")
    BigDecimal additionalPrice;

    @Schema(example = "20")
    @NotNull
    @Min(0)
    Integer stockQuantity;

    @Size(max = 255)
    String imageUrl;

    @Size(max = 255)
    String imagePublicId;

    Boolean active;
}
