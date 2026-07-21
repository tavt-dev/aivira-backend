package com.tien.aivirabackend.domain.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlogRelatedProductResponse {
    Long id;
    String productName;
    String slug;
    String bookAuthor;
    String thumbnailUrl;
}
