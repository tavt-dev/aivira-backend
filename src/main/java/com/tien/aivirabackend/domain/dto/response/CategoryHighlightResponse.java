package com.tien.aivirabackend.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Homepage category highlight with active book count.")
public class CategoryHighlightResponse {
    @Schema(example = "12")
    Long categoryId;
    @Schema(example = "Programming")
    String categoryName;
    @Schema(example = "programming")
    String slug;
    String description;
    String imageUrl;
    String imagePublicId;
    Integer displayOrder;
    @Schema(example = "24")
    Long bookCount;
}
