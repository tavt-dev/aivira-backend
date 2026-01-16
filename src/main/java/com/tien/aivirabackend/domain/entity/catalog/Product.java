package com.tien.aivirabackend.domain.entity.catalog;

import com.tien.aivirabackend.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.Set;

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

    @Lob
    @Column(nullable = false)
    String description;

    @Column(length = 100)
    String brand;

    @Column(length = 100)
    String material;

    @Column(name = "thumbnail_url")
    String thumbnailUrl;

    @Column(name = "thumbnail_public_id")
    String thumbnailPublicId;

    /* PRICING */
    @Column(nullable = false, precision =  19, scale = 2)
    BigDecimal price;// giá base

    @Column(name = "original_price", precision = 19, scale = 2)
    BigDecimal originalPrice;

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    BigDecimal discountPercentage;

    @Column(precision = 10, scale = 2)
    BigDecimal weight; // in kg

    /* INVENTORY */
    @Column(name ="stock_quantity", nullable = false)
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

    /* RELATIONSHIP */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    Set<ProductImage> productImages = Set.of();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    Set<ProductVariation> productVariations = Set.of();
}
