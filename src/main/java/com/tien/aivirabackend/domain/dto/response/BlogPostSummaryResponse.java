package com.tien.aivirabackend.domain.dto.response;

import java.time.Instant;

import com.tien.aivirabackend.constant.BlogPostStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlogPostSummaryResponse {
    Long id;
    String title;
    String slug;
    String excerpt;
    BlogPostStatus status;
    String coverUrl;
    String coverAltText;
    BlogCategoryResponse category;
    BlogAuthorResponse author;
    Instant publishedAt;
    Instant createdAt;
    Instant updatedAt;
}
