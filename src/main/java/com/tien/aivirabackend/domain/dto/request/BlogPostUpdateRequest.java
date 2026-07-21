package com.tien.aivirabackend.domain.dto.request;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlogPostUpdateRequest {
    @NotBlank
    @Size(min = 5, max = 255)
    String title;

    @Size(max = 255)
    String slug;

    @NotBlank
    @Size(max = 500)
    String excerpt;

    @NotBlank
    String contentHtml;

    @NotNull
    Long categoryId;

    @Size(max = 70)
    String seoTitle;

    @Size(max = 160)
    String metaDescription;

    @Builder.Default
    Set<Long> relatedProductIds = new HashSet<>();

    @Size(max = 255)
    String coverAltText;
}
