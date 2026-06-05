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
public class ReviewImageResponse {
    Long id;
    String imageUrl;
    String imagePublicId;
    Integer sortOrder;
    Instant createdAt;
    Instant updatedAt;
}
