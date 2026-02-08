package com.tien.aivirabackend.domain.entity.catalog;

import java.math.BigDecimal;

import jakarta.persistence.*;

import com.tien.aivirabackend.domain.entity.BaseEntity;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "product_variations")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductVariation extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    Long id;

    @Column(length = 50, nullable = false, unique = true)
    String sku;

    /* VARIATION ATTRIBUTES */
    @Column(nullable = false, length = 50)
    String color;

    @Column(nullable = false, length = 50)
    String size;

    /* PRICING */
    @Builder.Default
    @Column(nullable = false, name = "additional_price", precision = 19, scale = 2)
    BigDecimal additionalPrice = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, name = "stock_quantity")
    Integer stockQuantity = 0;

    @Column(name = "image_url")
    String imageUrl;

    @Column(name = "image_public_id")
    String imagePublicId;

    /* STATUS */
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    Boolean active = true;

    /* RELATIONSHIPS */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    Product product;
}
