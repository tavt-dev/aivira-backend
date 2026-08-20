package com.tien.aivirabackend.service.review;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.ReviewCreateRequest;
import com.tien.aivirabackend.domain.dto.request.ReviewModerateRequest;
import com.tien.aivirabackend.domain.dto.request.ReviewReplyRequest;
import com.tien.aivirabackend.domain.dto.request.ReviewUpdateRequest;
import com.tien.aivirabackend.domain.dto.response.ReviewResponse;

public interface ReviewService {
    PageResponse<ReviewResponse> getPublicReviews(String productSlug, Integer rating, String sort, int page, int size);

    ReviewResponse createReview(Long orderId, Long orderItemId, ReviewCreateRequest request);

    ReviewResponse createReviewWithImages(Long orderId, Long orderItemId, ReviewCreateRequest request,
            List<MultipartFile> imageFiles);

    ReviewResponse updateReview(Long reviewId, ReviewUpdateRequest request);

    void deleteReview(Long reviewId);

    PageResponse<ReviewResponse> getAdminReviews(Boolean approved, Boolean visible, Integer rating, String keyword,
            Long productId, String userId, int page, int size);

    ReviewResponse moderateReview(Long reviewId, ReviewModerateRequest request);

    ReviewResponse replyToReview(Long reviewId, ReviewReplyRequest request);
}
