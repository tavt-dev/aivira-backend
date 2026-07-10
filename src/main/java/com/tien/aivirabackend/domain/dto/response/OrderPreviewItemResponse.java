package com.tien.aivirabackend.domain.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderPreviewItemResponse {
    Long productId;
    String productName;
    String thumbnailUrl;
}
