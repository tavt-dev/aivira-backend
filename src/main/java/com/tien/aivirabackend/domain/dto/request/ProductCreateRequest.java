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

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(
        description =
                "Request to create a bookstore product. Backend resource name remains Product, but fields describe a book.")
public class ProductCreateRequest {
    @Schema(example = "BOOK-CLN-CODE-PB")
    @NotBlank
    @Size(max = 50)
    String sku;

    @Schema(example = "Clean Code")
    @NotBlank
    @Size(max = 255)
    String productName;

    @Schema(example = "clean-code")
    @Size(max = 255)
    String slug;

    @Schema(example = "A practical handbook for writing maintainable software.")
    @NotBlank
    String description;

    @Size(max = 100)
    String brand;

    @Size(max = 100)
    String material;

    @Schema(description = "Book author is required for newly created books.", example = "Robert C. Martin")
    @NotBlank
    @Size(max = 255)
    String bookAuthor;

    @Schema(description = "Optional ISBN, unique when provided. Stored as trimmed input.", example = "9780132350884")
    @Size(max = 20)
    String isbn;

    @Schema(example = "Prentice Hall")
    @Size(max = 255)
    String publisher;

    @Schema(description = "Publication year from 1000 to next calendar year.", example = "2008")
    Integer publicationYear;

    @Schema(example = "English")
    @Size(max = 80)
    String bookLanguage;

    @Schema(example = "464")
    @Positive
    Integer pageCount;

    @Schema(description = "Defaults to PAPERBACK when omitted.", example = "PAPERBACK")
    BookFormat bookFormat;

    @Schema(example = "17.8 x 2.5 x 23.1 cm")
    @Size(max = 120)
    String dimensions;

    @Schema(description = "Book category id.", example = "12")
    @NotNull
    Long categoryId;

    @Schema(example = "320000")
    @NotNull
    @DecimalMin("0.00")
    BigDecimal price;

    @DecimalMin("0.00")
    BigDecimal originalPrice;

    @DecimalMin("0.00")
    BigDecimal discountPercentage;

    @DecimalMin("0.00")
    BigDecimal weight;

    @Schema(description = "At least one variation is required, for example paperback or hardcover.")
    @Valid
    @NotEmpty
    List<ProductVariationRequest> variations;
}
