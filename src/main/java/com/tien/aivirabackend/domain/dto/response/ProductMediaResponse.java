package com.tien.aivirabackend.domain.dto.response;

import java.time.Instant;

import com.tien.aivirabackend.constant.MediaType;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductMediaResponse {
    Long id;
    String mediaUrl;
    String mediaPublicId;
    MediaType mediaType;
    String altText;
    Integer sortOrder;
    Boolean primary;
    Boolean active;
    Instant createdAt;
    Instant updatedAt;
}
