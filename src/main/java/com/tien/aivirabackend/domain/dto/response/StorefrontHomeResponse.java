package com.tien.aivirabackend.domain.dto.response;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Aggregated public bookstore homepage payload.")
public class StorefrontHomeResponse {
    @Schema(description = "Featured active books, newest first.")
    @Builder.Default
    List<ProductResponse> featuredBooks = new ArrayList<>();

    @Schema(description = "Newest active books.")
    @Builder.Default
    List<ProductResponse> newArrivals = new ArrayList<>();

    @Schema(description = "Best-selling active books by sold count.")
    @Builder.Default
    List<ProductResponse> bestsellingBooks = new ArrayList<>();

    @Schema(description = "Public categories with active book counts.")
    @Builder.Default
    List<CategoryHighlightResponse> categoryHighlights = new ArrayList<>();

    @Schema(description = "Latest four published blog posts.")
    @Builder.Default
    List<BlogPostSummaryResponse> latestPosts = new ArrayList<>();
}
