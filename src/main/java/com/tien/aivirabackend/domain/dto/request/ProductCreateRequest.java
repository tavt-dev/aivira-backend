package com.tien.aivirabackend.domain.dto.request;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class ProductCreateRequest {
    @NotBlank
    @Size(max = 50)
    String sku;

    @NotBlank
    @Size(max = 255)
    String productName;

    @Size(max = 255)
    String slug;

    @NotBlank
    String description;

    @Size(max = 100)
    String brand;

    @Size(max = 100)
    String material;

    @NotBlank
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

    @NotNull
    Long categoryId;

    @NotNull
    @DecimalMin("0.00")
    BigDecimal price;

    @DecimalMin("0.00")
    BigDecimal originalPrice;

    @DecimalMin("0.00")
    BigDecimal discountPercentage;

    @DecimalMin("0.00")
    BigDecimal weight;

    @Valid
    @NotEmpty
    List<ProductVariationRequest> variations;
}
