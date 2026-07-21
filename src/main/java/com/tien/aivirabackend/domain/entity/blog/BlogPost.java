package com.tien.aivirabackend.domain.entity.blog;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.tien.aivirabackend.constant.BlogPostStatus;
import com.tien.aivirabackend.domain.entity.BaseEntity;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.entity.user.User;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "blog_posts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlogPost extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 255)
    String title;

    @Column(nullable = false, unique = true, length = 255)
    String slug;

    @Column(nullable = false, length = 500)
    String excerpt;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "content_html", nullable = false, columnDefinition = "LONGTEXT")
    String contentHtml;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    BlogPostStatus status = BlogPostStatus.DRAFT;

    @Column(name = "cover_url", length = 1000)
    String coverUrl;

    @Column(name = "cover_public_id", length = 255)
    String coverPublicId;

    @Column(name = "cover_alt_text", length = 255)
    String coverAltText;

    @Column(name = "seo_title", length = 70)
    String seoTitle;

    @Column(name = "meta_description", length = 160)
    String metaDescription;

    @Column(name = "published_at")
    Instant publishedAt;

    @Column(name = "deleted_at")
    Instant deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    BlogCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", nullable = false)
    User updatedBy;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "blog_post_products",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id"))
    @Builder.Default
    Set<Product> relatedProducts = new HashSet<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<BlogAsset> assets = new ArrayList<>();
}
