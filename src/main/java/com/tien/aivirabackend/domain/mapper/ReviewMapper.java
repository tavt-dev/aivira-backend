package com.tien.aivirabackend.domain.mapper;

import java.util.Comparator;

import org.springframework.stereotype.Component;

import com.tien.aivirabackend.domain.dto.response.ReviewImageResponse;
import com.tien.aivirabackend.domain.dto.response.ReviewResponse;
import com.tien.aivirabackend.domain.entity.review.Review;
import com.tien.aivirabackend.domain.entity.review.ReviewImage;

@Component
public class ReviewMapper {
    public ReviewResponse toResponse(Review review) {
        if (review == null) {
            return null;
        }
        var orderItem = review.getOrderItem();
        return ReviewResponse.builder().id(review.getId()).rating(review.getRating()).comment(review.getComment())
                .approved(review.isApproved()).visible(review.isVisible()).adminReply(review.getAdminReply())
                .productId(review.getProduct() == null ? null : review.getProduct().getId())
                .productVariationId(review.getProductVariation() == null ? null : review.getProductVariation().getId())
                .orderId(review.getOrder() == null ? null : review.getOrder().getId())
                .orderItemId(orderItem == null ? null : orderItem.getId())
                .productName(orderItem == null ? null : orderItem.getProductName())
                .sku(orderItem == null ? null : orderItem.getSku())
                .username(review.getUser() == null ? null : review.getUser().getUsername())
                .userId(review.getUser() == null ? null : review.getUser().getId()).moderatedBy(review.getModeratedBy())
                .moderatedAt(review.getModeratedAt()).repliedBy(review.getRepliedBy()).repliedAt(review.getRepliedAt())
                .deletedAt(review.getDeletedAt()).createdAt(review.getCreatedAt()).updatedAt(review.getUpdatedAt())
                .images(review.getImages().stream()
                        .sorted(Comparator.comparing(ReviewImage::getSortOrder).thenComparing(ReviewImage::getId))
                        .map(this::toImageResponse).toList())
                .build();
    }

    private ReviewImageResponse toImageResponse(ReviewImage image) {
        return ReviewImageResponse.builder().id(image.getId()).imageUrl(image.getImageUrl())
                .imagePublicId(image.getImagePublicId()).sortOrder(image.getSortOrder()).createdAt(image.getCreatedAt())
                .updatedAt(image.getUpdatedAt()).build();
    }
}
