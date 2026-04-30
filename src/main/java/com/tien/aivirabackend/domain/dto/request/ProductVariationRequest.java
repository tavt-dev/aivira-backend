package com.tien.aivirabackend.domain.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductVariationRequest {
    @NotBlank
    @Size(max = 50)
    String sku;

    @NotBlank
    @Size(max = 50)
    String color;

    @NotBlank
    @Size(max = 50)
    String size;

    @NotNull
    @DecimalMin("0.00")
    BigDecimal additionalPrice;

    @NotNull
    @Min(0)
    Integer stockQuantity;

    @Size(max = 255)
    String imageUrl;

    @Size(max = 255)
    String imagePublicId;

    Boolean active;
}
