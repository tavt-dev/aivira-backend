package com.tien.aivirabackend.service.review;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.tien.aivirabackend.constant.OrderStatus;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.ReviewCreateRequest;
import com.tien.aivirabackend.domain.dto.request.ReviewImageRequest;
import com.tien.aivirabackend.domain.dto.request.ReviewModerateRequest;
import com.tien.aivirabackend.domain.dto.request.ReviewReplyRequest;
import com.tien.aivirabackend.domain.dto.request.ReviewUpdateRequest;
import com.tien.aivirabackend.domain.dto.response.ReviewResponse;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.entity.catalog.ProductVariation;
import com.tien.aivirabackend.domain.entity.review.Review;
import com.tien.aivirabackend.domain.entity.review.ReviewImage;
import com.tien.aivirabackend.domain.entity.transaction.Order;
import com.tien.aivirabackend.domain.entity.transaction.OrderItem;
import com.tien.aivirabackend.domain.mapper.ReviewMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.OrderErrorCode;
import com.tien.aivirabackend.exception.errorCode.ReviewErrorCode;
import com.tien.aivirabackend.repository.OrderRepository;
import com.tien.aivirabackend.repository.ProductRepository;
import com.tien.aivirabackend.repository.ProductVariationRepository;
import com.tien.aivirabackend.repository.ReviewRepository;
import com.tien.aivirabackend.service.auth.CurrentUserService;
import com.tien.aivirabackend.util.PageRequestUtils;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j(topic = "REVIEW-SERVICE")
public class ReviewServiceImpl implements ReviewService {
    private static final int MAX_IMAGES = 5;

    ReviewRepository reviewRepository;
    OrderRepository orderRepository;
    ProductRepository productRepository;
    ProductVariationRepository productVariationRepository;
    CurrentUserService currentUserService;
    ReviewSpecifications reviewSpecifications;
    ReviewMapper reviewMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getPublicReviews(
            String productSlug, Integer rating, String sort, int page, int size) {
        var pageable = PageRequestUtils.of(page, size, reviewSort(sort));
        return PageResponse.from(reviewRepository
                .findAll(reviewSpecifications.publicReviews(productSlug, rating), pageable)
                .map(reviewMapper::toResponse));
    }

    @Override
    @Transactional
    public ReviewResponse createReview(Long orderId, Long orderItemId, ReviewCreateRequest request) {
        String userId = currentUserService.getCurrentUserId();
        Order order = orderRepository
                .findDetailedByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));
        if (order.getOrderStatus() != OrderStatus.COMPLETED) {
            throw new AppException(ReviewErrorCode.REVIEW_ORDER_NOT_COMPLETED);
        }
        OrderItem orderItem = findOrderItem(order, orderItemId);
        if (reviewRepository.existsByOrderItem_Id(orderItemId)) {
            throw new AppException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
        }
        Product product = productRepository
                .findById(orderItem.getProductId())
                .orElseThrow(() -> new AppException(ReviewErrorCode.REVIEW_NOT_ALLOWED));
        if (orderItem.getProductVariationId() == null) {
            throw new AppException(ReviewErrorCode.REVIEW_NOT_ALLOWED);
        }
        ProductVariation variation = productVariationRepository
                .findById(orderItem.getProductVariationId())
                .orElseThrow(() -> new AppException(ReviewErrorCode.REVIEW_NOT_ALLOWED));
        if (!Objects.equals(variation.getProduct().getId(), product.getId())) {
            throw new AppException(ReviewErrorCode.REVIEW_NOT_ALLOWED);
        }

        Review review = Review.builder()
                .rating(request.getRating())
                .comment(trimToNull(request.getComment()))
                .approved(false)
                .visible(true)
                .user(order.getUser())
                .product(product)
                .productVariation(variation)
                .order(order)
                .orderItem(orderItem)
                .build();
        replaceImages(review, request.getImages());
        return reviewMapper.toResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(Long reviewId, ReviewUpdateRequest request) {
        String userId = currentUserService.getCurrentUserId();
        Review review = reviewRepository
                .findDetailedByIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new AppException(ReviewErrorCode.REVIEW_NOT_FOUND));
        rejectDeleted(review);
        review.setRating(request.getRating());
        review.setComment(trimToNull(request.getComment()));
        review.setApproved(false);
        review.setModeratedBy(null);
        review.setModeratedAt(null);
        replaceImages(review, request.getImages());
        return reviewMapper.toResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        String userId = currentUserService.getCurrentUserId();
        Review review = reviewRepository
                .findDetailedByIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new AppException(ReviewErrorCode.REVIEW_NOT_FOUND));
        if (review.getDeletedAt() != null) {
            return;
        }
        review.setVisible(false);
        review.setDeletedAt(Instant.now());
        reviewRepository.save(review);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getAdminReviews(
            Boolean approved,
            Boolean visible,
            Integer rating,
            String keyword,
            Long productId,
            String userId,
            int page,
            int size) {
        var pageable = PageRequestUtils.newestFirst(page, size);
        return PageResponse.from(reviewRepository
                .findAll(
                        reviewSpecifications.adminReviews(approved, visible, rating, keyword, productId, userId),
                        pageable)
                .map(reviewMapper::toResponse));
    }

    @Override
    @Transactional
    public ReviewResponse moderateReview(Long reviewId, ReviewModerateRequest request) {
        Review review = findDetailedReview(reviewId);
        Instant now = Instant.now();
        review.setApproved(Boolean.TRUE.equals(request.getApproved()));
        review.setVisible(Boolean.TRUE.equals(request.getVisible()));
        review.setModeratedBy(resolveAdminId());
        review.setModeratedAt(now);
        log.info(
                "admin_review_moderated reviewId={} approved={} visible={} adminUserId={}",
                review.getId(),
                review.isApproved(),
                review.isVisible(),
                review.getModeratedBy());
        return reviewMapper.toResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional
    public ReviewResponse replyToReview(Long reviewId, ReviewReplyRequest request) {
        Review review = findDetailedReview(reviewId);
        String reply = trimToNull(request == null ? null : request.getAdminReply());
        review.setAdminReply(reply);
        if (reply == null) {
            review.setRepliedBy(null);
            review.setRepliedAt(null);
        } else {
            review.setRepliedBy(resolveAdminId());
            review.setRepliedAt(Instant.now());
        }
        return reviewMapper.toResponse(reviewRepository.save(review));
    }

    private Review findDetailedReview(Long reviewId) {
        return reviewRepository
                .findDetailedById(reviewId)
                .orElseThrow(() -> new AppException(ReviewErrorCode.REVIEW_NOT_FOUND));
    }

    private OrderItem findOrderItem(Order order, Long orderItemId) {
        return order.getItems().stream()
                .filter(item -> Objects.equals(item.getId(), orderItemId))
                .findFirst()
                .orElseThrow(() -> new AppException(ReviewErrorCode.REVIEW_NOT_ALLOWED));
    }

    private void replaceImages(Review review, List<ReviewImageRequest> imageRequests) {
        review.getImages().clear();
        if (imageRequests == null || imageRequests.isEmpty()) {
            return;
        }
        if (imageRequests.size() > MAX_IMAGES) {
            throw new AppException(ReviewErrorCode.REVIEW_INVALID_IMAGE);
        }
        for (ReviewImageRequest request : imageRequests) {
            if (!StringUtils.hasText(request.getImageUrl()) || !StringUtils.hasText(request.getImagePublicId())) {
                throw new AppException(ReviewErrorCode.REVIEW_INVALID_IMAGE);
            }
            review.getImages()
                    .add(ReviewImage.builder()
                            .review(review)
                            .imageUrl(request.getImageUrl().trim())
                            .imagePublicId(request.getImagePublicId().trim())
                            .sortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder())
                            .build());
        }
    }

    private void rejectDeleted(Review review) {
        if (review.getDeletedAt() != null) {
            throw new AppException(ReviewErrorCode.REVIEW_DELETED);
        }
    }

    private Sort reviewSort(String sort) {
        if (!StringUtils.hasText(sort) || "newest".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return switch (sort.trim().toLowerCase()) {
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "rating_desc" -> Sort.by(Sort.Direction.DESC, "rating");
            case "rating_asc" -> Sort.by(Sort.Direction.ASC, "rating");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private String resolveAdminId() {
        return currentUserService.findCurrentUserId().orElse("UNKNOWN");
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
