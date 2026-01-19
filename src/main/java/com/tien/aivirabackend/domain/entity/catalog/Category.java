package com.tien.aivirabackend.domain.entity.catalog;

import com.tien.aivirabackend.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "categories")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Category extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /* BASIC INFO */
    @Column(nullable = false, unique = true, name = "category_name", length = 150)
    String categoryName;

    @Column(unique = true, length = 150, nullable = false)
    String slug;

    @Column(nullable = false, length = 1000)
    String description;

    @Column(name = "image_url")
    String imageUrl;

    @Column(name = "image_public_id")
    String imagePublicId;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    Integer displayOrder = 0;

    /* CATEGORY TREE */
    @ManyToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    Category parentCategory;

    @OneToMany(mappedBy = "parentCategory", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    Set<Category> childCategories = new HashSet<>();

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    Set<Product> products = new HashSet<>();

    /* STATUS */
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    Boolean active = true;

    @Builder.Default
    @Column(name = "is_visible", nullable = false)
    Boolean visible = true;
}
