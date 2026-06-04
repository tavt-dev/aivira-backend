package com.tien.aivirabackend.domain.entity.catalog;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.tien.aivirabackend.constant.BookFormat;
import com.tien.aivirabackend.constant.ProductStatus;
import com.tien.aivirabackend.domain.entity.BaseEntity;
import com.tien.aivirabackend.domain.entity.review.Review;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "products")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Product extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /* BASIC INFO */
    @Column(nullable = false, unique = true, length = 50)
    String sku;

    @Column(nullable = false, name = "product_name", length = 255)
    String productName;

    @Column(length = 255, unique = true)
    String slug;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    String description;

    @Column(length = 100)
    String brand;

    @Column(length = 100)
    String material;

    @Column(name = "book_author", nullable = false, length = 255)
    @Builder.Default
    String bookAuthor = "Unknown";

    @Column(length = 20, unique = true)
    String isbn;

    @Column(length = 255)
    String publisher;

    @Column(name = "publication_year")
    Integer publicationYear;

    @Column(name = "book_language", length = 80)
    String bookLanguage;

    @Column(name = "page_count")
    Integer pageCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "book_format", nullable = false, length = 50)
    @Builder.Default
    BookFormat bookFormat = BookFormat.PAPERBACK;

    @Column(length = 120)
    String dimensions;

    @Column(name = "thumbnail_url")
    String thumbnailUrl;

    @Column(name = "thumbnail_public_id")
    String thumbnailPublicId;

    /* PRICING */
    @Column(nullable = false, precision = 19, scale = 2)
    BigDecimal price; // giá base

    @Column(name = "original_price", precision = 19, scale = 2)
    BigDecimal originalPrice;

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    BigDecimal discountPercentage;

    @Column(precision = 10, scale = 2)
    BigDecimal weight; // in kg

    /* INVENTORY */
    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    Integer stockQuantity = 0;

    @Column(name = "sold_count", nullable = false)
    @Builder.Default
    Integer soldCount = 0;

    /* STATUS */
    @Column(name = "is_active")
    @Builder.Default
    Boolean active = true;

    @Column(name = "is_featured")
    @Builder.Default
    Boolean featured = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    ProductStatus status = ProductStatus.DRAFT;

    @Column(name = "rejection_reason", length = 500)
    String rejectionReason;

    @Column(name = "submitted_at")
    java.time.Instant submittedAt;

    @Column(name = "approved_by")
    String approvedBy;

    @Column(name = "approved_at")
    java.time.Instant approvedAt;

    @Column(name = "rejected_by")
    String rejectedBy;

    @Column(name = "rejected_at")
    java.time.Instant rejectedAt;

    /* RELATIONSHIP */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    Set<ProductMedia> productMedia = new HashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    Set<ProductVariation> productVariations = new HashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    Set<Review> reviews = new HashSet<>();
}
