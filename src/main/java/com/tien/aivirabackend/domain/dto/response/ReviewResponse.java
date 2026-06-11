package com.tien.aivirabackend.domain.dto.response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Book review response. Public lists include only approved, visible, non-deleted reviews.")
public class ReviewResponse {
    @Schema(example = "42")
    Long id;

    @Schema(example = "5")
    Integer rating;

    @Schema(example = "Excellent book quality and fast delivery.")
    String comment;

    @Schema(example = "true")
    Boolean approved;

    @Schema(example = "true")
    Boolean visible;

    @Schema(description = "Admin reply only. No shop/seller reply exists.", example = "Thank you for your feedback.")
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
