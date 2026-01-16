package com.tien.aivirabackend.domain.entity.transaction;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    Order order;

    @Column(name = "product_id", nullable = false)
    Long productId;

    @Column(name = "product_variation_id")
    Long productVariationId;

    @Column(name = "product_name", nullable = false, length = 255)
    String productName;

    @Column(name = "sku", nullable = false, length = 50)
    String sku;

    @Column(name = "variation_color", length = 50)
    String variationColor;

    @Column(name = "variation_size", length = 50)
    String variationSize;

    @Column(name = "thumbnail_url")
    String thumbnailUrl;

    @Column(name = "base_price", nullable = false, precision = 19, scale = 2)
    BigDecimal basePrice; // product.price

    @Column(name = "additional_price", precision = 19, scale = 2)
    @Builder.Default
    BigDecimal additionalPrice = BigDecimal.ZERO; // variation.additionalPrice

    @Column(name = "discount_amount", precision = 19, scale = 2)
    @Builder.Default
    BigDecimal discountAmount = BigDecimal.ZERO; // promotion / coupon

    @Column(name = "final_price", nullable = false, precision = 19, scale = 2)
    BigDecimal finalPrice;

    @Column(name = "promotion_name", length = 255)
    String promotionName;

    @Column(nullable = false)
    Integer quantity;
}
