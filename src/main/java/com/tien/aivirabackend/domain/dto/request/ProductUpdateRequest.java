package com.tien.aivirabackend.domain.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.tien.aivirabackend.constant.BookFormat;

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

    @Size(max = 255)
    String bookAuthor;

    @Size(max = 20)
    String isbn;

    @Size(max = 255)
    String publisher;

    Integer publicationYear;

    @Size(max = 80)
    String bookLanguage;

    @Positive
    Integer pageCount;

    BookFormat bookFormat;

    @Size(max = 120)
    String dimensions;

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
