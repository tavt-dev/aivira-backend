package com.tien.aivirabackend.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.ReviewCreateRequest;
import com.tien.aivirabackend.domain.dto.request.ReviewUpdateRequest;
import com.tien.aivirabackend.domain.dto.response.ReviewResponse;
import com.tien.aivirabackend.service.review.ReviewService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReviewController {
    ReviewService reviewService;

    @GetMapping("/products/{slug}/reviews")
    @Tag(name = "Public Reviews")
    @Operation(
            summary = "Get public book reviews",
            description = "Returns approved, visible, non-deleted reviews for a public book slug.")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getPublicReviews(
            @PathVariable String slug,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                "Get reviews successful", reviewService.getPublicReviews(slug, rating, sort, page, size)));
    }

    @PostMapping("/orders/{orderId}/items/{orderItemId}/review")
    @Tag(name = "Customer Reviews")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Create review for completed order item",
            description =
                    "Creates one pending review for a purchased order item. The order must belong to the current user and be COMPLETED.")
    @PreAuthorize("@authorizationService.hasPermission('REVIEW_CREATE_SELF')")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @PathVariable Long orderId,
            @PathVariable Long orderItemId,
            @Valid @RequestBody ReviewCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Create review successful", reviewService.createReview(orderId, orderItemId, request)));
    }

    @PutMapping("/reviews/{reviewId}")
    @Tag(name = "Customer Reviews")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Update own review",
            description = "Updates the current user's review and resets approval until admin moderation.")
    @PreAuthorize("@authorizationService.hasPermission('REVIEW_UPDATE_SELF')")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable Long reviewId, @Valid @RequestBody ReviewUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Update review successful", reviewService.updateReview(reviewId, request)));
    }

    @DeleteMapping("/reviews/{reviewId}")
    @Tag(name = "Customer Reviews")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Delete own review",
            description = "Soft-deletes the current user's review so order history remains intact.")
    @PreAuthorize("@authorizationService.hasPermission('REVIEW_DELETE_SELF')")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Delete review successful", null));
    }
}
