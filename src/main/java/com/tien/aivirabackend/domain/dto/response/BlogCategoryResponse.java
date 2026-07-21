package com.tien.aivirabackend.domain.dto.response;

import java.time.Instant;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlogCategoryResponse {
    Long id;
    String name;
    String slug;
    String description;
    Integer displayOrder;
    Boolean active;
    Instant createdAt;
    Instant updatedAt;
}
