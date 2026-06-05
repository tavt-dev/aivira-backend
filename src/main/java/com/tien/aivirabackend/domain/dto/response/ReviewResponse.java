package com.tien.aivirabackend.domain.dto.response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewResponse {
    Long id;
    Integer rating;
    String comment;
    Boolean approved;
    Boolean visible;
    String adminReply;
    Long productId;
    Long productVariationId;
    Long orderId;
    Long orderItemId;
    String productName;
    String sku;
    String username;
    String userId;
    String moderatedBy;
    Instant moderatedAt;
    String repliedBy;
    Instant repliedAt;
    Instant deletedAt;
    Instant createdAt;
    Instant updatedAt;

    @Builder.Default
    List<ReviewImageResponse> images = new ArrayList<>();
}
