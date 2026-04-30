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
public class CategoryResponse {
    Long id;
    String categoryName;
    String slug;
    String description;
    String imageUrl;
    String imagePublicId;
    Integer displayOrder;
    Long parentId;
    Boolean active;
    Boolean visible;
    Instant createdAt;
    Instant updatedAt;

    @Builder.Default
    List<CategoryResponse> children = new ArrayList<>();
}
