package com.tien.aivirabackend.domain.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.tien.aivirabackend.constant.BookFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Partial update request for an admin-managed bookstore product. Null fields are left unchanged.")
public class ProductUpdateRequest {
    @Schema(example = "BOOK-CLN-CODE-PB")
    @Size(max = 50)
    String sku;

    @Schema(example = "Clean Code")
    @Size(max = 255)
    String productName;

    @Size(max = 255)
    String slug;

    String description;

    @Size(max = 100)
    String brand;

    @Size(max = 100)
    String material;

    @Schema(description = "When provided, must not be blank.", example = "Robert C. Martin")
    @Size(max = 255)
    String bookAuthor;

    @Schema(description = "Blank clears ISBN; nonblank value must be unique.", example = "9780132350884")
    @Size(max = 20)
    String isbn;

    @Schema(example = "Prentice Hall")
    @Size(max = 255)
    String publisher;

    @Schema(description = "Publication year from 1000 to next calendar year.", example = "2008")
    Integer publicationYear;

    @Size(max = 80)
    String bookLanguage;

    @Positive
    Integer pageCount;

    @Schema(example = "PAPERBACK")
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
