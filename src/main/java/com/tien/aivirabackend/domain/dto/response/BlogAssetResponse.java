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
public class BlogAssetResponse {
    Long id;
    String url;
    String publicId;
    String altText;
    Instant createdAt;
}
