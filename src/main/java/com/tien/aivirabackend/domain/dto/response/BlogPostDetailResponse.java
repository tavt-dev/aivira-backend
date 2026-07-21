package com.tien.aivirabackend.domain.dto.response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlogPostDetailResponse {
    Long id;
    String title;
    String slug;
    String excerpt;
    String contentHtml;
    String coverUrl;
    String coverAltText;
    String seoTitle;
    String metaDescription;
    BlogCategoryResponse category;
    BlogAuthorResponse author;
    Instant publishedAt;
    Instant updatedAt;

    @Builder.Default
    List<BlogRelatedProductResponse> relatedProducts = new ArrayList<>();
}
