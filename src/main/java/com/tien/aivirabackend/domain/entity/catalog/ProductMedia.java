package com.tien.aivirabackend.domain.entity.catalog;

import com.tien.aivirabackend.constant.MediaType;
import jakarta.persistence.*;

import com.tien.aivirabackend.domain.entity.BaseEntity;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "product_media")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductMedia extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "media_url", nullable = false, length = 1000)
    String mediaUrl;

    @Column(name = "media_public_id", nullable = false, length = 255)
    String mediaPublicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    MediaType mediaType;

    @Column(name = "alt_text", length = 255)
    String altText;

    @Column(name = "sort_order", nullable = false)
    Integer sortOrder;

    @Column(name = "is_primary", nullable = false)
    Boolean primary;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    Product product;
}
