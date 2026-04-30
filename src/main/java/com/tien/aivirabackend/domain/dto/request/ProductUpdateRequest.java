package com.tien.aivirabackend.domain.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductUpdateRequest {
    @Size(max = 50)
    String sku;

    @Size(max = 255)
    String productName;

    @Size(max = 255)
    String slug;

    String description;

    @Size(max = 100)
    String brand;

    @Size(max = 100)
    String material;

    Long categoryId;

    @DecimalMin("0.00")
    BigDecimal price;

    @DecimalMin("0.00")
    BigDecimal originalPrice;

    @DecimalMin("0.00")
    BigDecimal discountPercentage;

    @DecimalMin("0.00")
    BigDecimal weight;

    Boolean featured;
}
