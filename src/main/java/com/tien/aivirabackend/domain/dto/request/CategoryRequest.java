package com.tien.aivirabackend.domain.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryRequest {
    @NotBlank
    @Size(min = 2, max = 150)
    String categoryName;

    @Size(max = 150)
    String slug;

    @NotBlank
    @Size(max = 1000)
    String description;

    @Size(max = 255)
    String imageUrl;

    @Size(max = 255)
    String imagePublicId;

    @Min(0)
    Integer displayOrder;

    Long parentId;

    Boolean active;

    Boolean visible;
}
