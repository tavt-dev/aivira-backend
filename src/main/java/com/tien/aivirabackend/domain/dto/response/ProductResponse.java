package com.tien.aivirabackend.domain.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.tien.aivirabackend.constant.BookFormat;
import com.tien.aivirabackend.constant.ProductStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductResponse {
    Long id;
    Long categoryId;
    String categoryName;
    String categorySlug;
    String sku;
    String productName;
    String slug;
    String description;
    String brand;
    String material;
    String bookAuthor;
    String isbn;
    String publisher;
    Integer publicationYear;
    String bookLanguage;
    Integer pageCount;
    BookFormat bookFormat;
    String dimensions;
    String thumbnailUrl;
    String thumbnailPublicId;
    BigDecimal price;
    BigDecimal originalPrice;
    BigDecimal discountPercentage;
    BigDecimal weight;
    Integer stockQuantity;
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
