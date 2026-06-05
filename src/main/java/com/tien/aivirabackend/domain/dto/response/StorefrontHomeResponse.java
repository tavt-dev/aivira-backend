package com.tien.aivirabackend.domain.dto.response;

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
public class StorefrontHomeResponse {
    @Builder.Default
    List<ProductResponse> featuredBooks = new ArrayList<>();

    @Builder.Default
    List<ProductResponse> newArrivals = new ArrayList<>();

    @Builder.Default
    List<ProductResponse> bestsellingBooks = new ArrayList<>();

    @Builder.Default
    List<CategoryHighlightResponse> categoryHighlights = new ArrayList<>();
}
