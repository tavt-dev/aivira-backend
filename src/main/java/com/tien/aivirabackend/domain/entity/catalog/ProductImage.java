package com.tien.aivirabackend.domain.entity.catalog;

import jakarta.persistence.*;

import com.tien.aivirabackend.domain.entity.BaseEntity;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "product_images")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductImage extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /* IMAGE INFO */
    @Column(name = "image_url", nullable = false)
    String imageUrl;

    @Column(name = "image_public_id", nullable = false)
    String imagePublicId;

    @Column(name = "alt_text", length = 255)
    String altText;

    /* DISPLAY ORDER */
    @Column(name = "sort_order", nullable = false)
    Integer sortOrder;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    Boolean active = true;

    /* RELATIONSHIPS */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    Product product;
}
