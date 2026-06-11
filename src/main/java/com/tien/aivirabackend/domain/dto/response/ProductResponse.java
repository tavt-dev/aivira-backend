package com.tien.aivirabackend.domain.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.tien.aivirabackend.constant.BookFormat;
import com.tien.aivirabackend.constant.ProductStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Book catalog response. Backend name remains Product for API stability.")
public class ProductResponse {
    @Schema(example = "101")
    Long id;

    Long categoryId;

    @Schema(example = "Programming")
    String categoryName;

    @Schema(example = "programming")
    String categorySlug;

    @Schema(example = "BOOK-CLN-CODE-PB")
    String sku;

    @Schema(example = "Clean Code")
    String productName;

    @Schema(example = "clean-code")
    String slug;

    String description;
    String brand;
    String material;

    @Schema(example = "Robert C. Martin")
    String bookAuthor;

    @Schema(example = "9780132350884")
    String isbn;

    @Schema(example = "Prentice Hall")
    String publisher;

    @Schema(example = "2008")
    Integer publicationYear;

    @Schema(example = "English")
    String bookLanguage;

    @Schema(example = "464")
    Integer pageCount;

    @Schema(example = "PAPERBACK")
    BookFormat bookFormat;

    String dimensions;
    String thumbnailUrl;
    String thumbnailPublicId;

    @Schema(example = "320000")
    BigDecimal price;

    BigDecimal originalPrice;
    BigDecimal discountPercentage;
    BigDecimal weight;

    @Schema(example = "18")
    Integer stockQuantity;

    @Schema(example = "42")
    Integer soldCount;

    Boolean active;
    Boolean featured;
    ProductStatus status;
    String rejectionReason;
    Instant submittedAt;
    String approvedBy;
    Instant approvedAt;
    String rejectedBy;
    Instant rejectedAt;
    Instant createdAt;
    Instant updatedAt;

    @Builder.Default
    List<ProductVariationResponse> variations = new ArrayList<>();

    @Builder.Default
    List<ProductMediaResponse> media = new ArrayList<>();
}
