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
public class BlogCategoryRequest {
    @NotBlank
    @Size(min = 2, max = 150)
    String name;

    @Size(max = 255)
    String slug;

    @Size(max = 1000)
    String description;

    @Min(0)
    Integer displayOrder;

    Boolean active;
}
