package com.tien.aivirabackend.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.ReviewModerateRequest;
import com.tien.aivirabackend.domain.dto.request.ReviewReplyRequest;
import com.tien.aivirabackend.domain.dto.response.ReviewResponse;
import com.tien.aivirabackend.service.review.ReviewService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/admin/reviews")
@Tag(name = "Admin Reviews")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminReviewController {
    ReviewService reviewService;

    @GetMapping
    @Operation(summary = "List reviews for admin")
    @PreAuthorize("@authorizationService.hasAnyPermission('REVIEW_MANAGE_ALL', 'REVIEW_READ_ALL')")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getAdminReviews(
            @RequestParam(required = false) Boolean approved,
            @RequestParam(required = false) Boolean visible,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                "Get admin reviews successful",
                reviewService.getAdminReviews(approved, visible, rating, keyword, productId, userId, page, size)));
    }

    @PutMapping("/{reviewId}/moderate")
    @Operation(summary = "Moderate review")
    @PreAuthorize("@authorizationService.hasAnyPermission('REVIEW_MANAGE_ALL', 'REVIEW_MODERATE')")
    public ResponseEntity<ApiResponse<ReviewResponse>> moderateReview(
            @PathVariable Long reviewId, @Valid @RequestBody ReviewModerateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Moderate review successful", reviewService.moderateReview(reviewId, request)));
    }

    @PutMapping("/{reviewId}/reply")
    @Operation(summary = "Reply to review")
    @PreAuthorize("@authorizationService.hasAnyPermission('REVIEW_MANAGE_ALL', 'REVIEW_MODERATE')")
    public ResponseEntity<ApiResponse<ReviewResponse>> replyToReview(
            @PathVariable Long reviewId, @Valid @RequestBody ReviewReplyRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Reply to review successful", reviewService.replyToReview(reviewId, request)));
    }
}
