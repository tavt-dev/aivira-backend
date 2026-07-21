package com.tien.aivirabackend.domain.dto.response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.tien.aivirabackend.constant.BlogPostStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlogPostResponse {
    Long id;
    String title;
    String slug;
    String excerpt;
    String contentHtml;
    BlogPostStatus status;
    String coverUrl;
    String coverPublicId;
    String coverAltText;
    String seoTitle;
    String metaDescription;
    BlogCategoryResponse category;
    BlogAuthorResponse author;
    String updatedBy;
    Instant publishedAt;
    Instant deletedAt;
    Instant createdAt;
    Instant updatedAt;

    @Builder.Default
    List<BlogRelatedProductResponse> relatedProducts = new ArrayList<>();

    @Builder.Default
    List<BlogAssetResponse> assets = new ArrayList<>();
}
