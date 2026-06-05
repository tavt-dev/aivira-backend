package com.tien.aivirabackend.domain.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryHighlightResponse {
    Long categoryId;
    String categoryName;
    String slug;
    String description;
    String imageUrl;
    String imagePublicId;
    Integer displayOrder;
    Long bookCount;
}
