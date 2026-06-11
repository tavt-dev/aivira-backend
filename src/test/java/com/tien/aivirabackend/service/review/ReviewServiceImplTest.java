package com.tien.aivirabackend.service.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.tien.aivirabackend.constant.OrderStatus;
import com.tien.aivirabackend.domain.dto.request.ReviewCreateRequest;
import com.tien.aivirabackend.domain.dto.request.ReviewImageRequest;
import com.tien.aivirabackend.domain.dto.request.ReviewModerateRequest;
import com.tien.aivirabackend.domain.dto.request.ReviewReplyRequest;
import com.tien.aivirabackend.domain.dto.request.ReviewUpdateRequest;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.entity.catalog.ProductVariation;
import com.tien.aivirabackend.domain.entity.review.Review;
import com.tien.aivirabackend.domain.entity.transaction.Order;
import com.tien.aivirabackend.domain.entity.transaction.OrderItem;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.mapper.ReviewMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.OrderErrorCode;
import com.tien.aivirabackend.exception.errorCode.ReviewErrorCode;
import com.tien.aivirabackend.repository.OrderRepository;
import com.tien.aivirabackend.repository.ProductRepository;
import com.tien.aivirabackend.repository.ProductVariationRepository;
import com.tien.aivirabackend.repository.ReviewRepository;
import com.tien.aivirabackend.service.auth.CurrentUserService;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {
    @Mock
    ReviewRepository reviewRepository;

    @Mock
    OrderRepository orderRepository;

    @Mock
    ProductRepository productRepository;

    @Mock
    ProductVariationRepository productVariationRepository;

    @Mock
    CurrentUserService currentUserService;

    ReviewServiceImpl reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewServiceImpl(
                reviewRepository,
                orderRepository,
                productRepository,
                productVariationRepository,
                currentUserService,
                new ReviewSpecifications(),
                new ReviewMapper());
    }

    @Test
    void getPublicReviews_shouldQueryApprovedVisibleNonDeletedReviews() {
        Review review = approvedReview();
        when(reviewRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review)));

        var response = reviewService.getPublicReviews("book-slug", 5, "rating_desc", 1, 20);

        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().getFirst().getRating()).isEqualTo(5);
        verify(reviewRepository)
                .findAll(
                        any(Specification.class),
                        argThat((Pageable pageable) -> pageable.getSort().getOrderFor("rating") != null));
    }

    @Test
    void createReview_whenCompletedOwnedOrderItem_shouldCreatePendingReview() {
        Order order = order(OrderStatus.COMPLETED);
        Product product = product();
        ProductVariation variation = variation(product);
        when(currentUserService.getCurrentUserId()).thenReturn("user-1");
        when(orderRepository.findDetailedByIdAndUserId(21L, "user-1")).thenReturn(Optional.of(order));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productVariationRepository.findById(11L)).thenReturn(Optional.of(variation));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setId(99L);
            return review;
        });

        var response = reviewService.createReview(21L, 31L, createRequest());

        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getApproved()).isFalse();
        assertThat(response.getVisible()).isTrue();
        assertThat(response.getImages()).hasSize(1);
        verify(reviewRepository)
                .save(argThat(review -> review.getOrderItem().getId().equals(31L)
                        && review.getProduct().getId().equals(10L)
                        && !review.isApproved()
                        && review.isVisible()));
    }

    @Test
    void createReview_whenOrderMissingForCurrentUser_shouldThrowOrderNotFound() {
        when(currentUserService.getCurrentUserId()).thenReturn("user-1");
        when(orderRepository.findDetailedByIdAndUserId(21L, "user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(21L, 31L, createRequest()))
                .isInstanceOfSatisfying(AppException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(OrderErrorCode.ORDER_NOT_FOUND));
    }

    @Test
    void createReview_whenOrderNotCompleted_shouldThrow() {
        when(currentUserService.getCurrentUserId()).thenReturn("user-1");
        when(orderRepository.findDetailedByIdAndUserId(21L, "user-1"))
                .thenReturn(Optional.of(order(OrderStatus.CONFIRMED)));

        assertThatThrownBy(() -> reviewService.createReview(21L, 31L, createRequest()))
                .isInstanceOfSatisfying(AppException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(ReviewErrorCode.REVIEW_ORDER_NOT_COMPLETED));
    }

    @Test
    void createReview_whenDuplicateOrderItem_shouldThrow() {
        when(currentUserService.getCurrentUserId()).thenReturn("user-1");
        when(orderRepository.findDetailedByIdAndUserId(21L, "user-1"))
                .thenReturn(Optional.of(order(OrderStatus.COMPLETED)));
        when(reviewRepository.existsByOrderItem_Id(31L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(21L, 31L, createRequest()))
                .isInstanceOfSatisfying(AppException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(ReviewErrorCode.REVIEW_ALREADY_EXISTS));
    }

    @Test
    void createReview_whenOrderItemNotInOrder_shouldThrow() {
        when(currentUserService.getCurrentUserId()).thenReturn("user-1");
        when(orderRepository.findDetailedByIdAndUserId(21L, "user-1"))
                .thenReturn(Optional.of(order(OrderStatus.COMPLETED)));

        assertThatThrownBy(() -> reviewService.createReview(21L, 999L, createRequest()))
                .isInstanceOfSatisfying(AppException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(ReviewErrorCode.REVIEW_NOT_ALLOWED));
    }

    @Test
    void updateReview_shouldResetApprovalAndReplaceImages() {
        Review review = approvedReview();
        when(currentUserService.getCurrentUserId()).thenReturn("user-1");
        when(reviewRepository.findDetailedByIdAndUserId(99L, "user-1")).thenReturn(Optional.of(review));
        when(reviewRepository.save(review)).thenReturn(review);

        var response = reviewService.updateReview(99L, updateRequest());

        assertThat(response.getApproved()).isFalse();
        assertThat(review.getModeratedBy()).isNull();
        assertThat(review.getImages()).hasSize(1);
        assertThat(review.getImages().getFirst().getImageUrl()).isEqualTo("https://cdn.example.com/new.jpg");
    }

    @Test
    void updateReview_whenDeleted_shouldThrow() {
        Review review = approvedReview();
        review.setDeletedAt(Instant.now());
        when(currentUserService.getCurrentUserId()).thenReturn("user-1");
        when(reviewRepository.findDetailedByIdAndUserId(99L, "user-1")).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.updateReview(99L, updateRequest()))
                .isInstanceOfSatisfying(AppException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(ReviewErrorCode.REVIEW_DELETED));
    }

    @Test
    void deleteReview_shouldSoftDelete() {
        Review review = approvedReview();
        when(currentUserService.getCurrentUserId()).thenReturn("user-1");
        when(reviewRepository.findDetailedByIdAndUserId(99L, "user-1")).thenReturn(Optional.of(review));

        reviewService.deleteReview(99L);

        assertThat(review.isVisible()).isFalse();
        assertThat(review.getDeletedAt()).isNotNull();
        verify(reviewRepository).save(review);
    }

    @Test
    void moderateReview_shouldSetApprovalVisibilityAndAdminMetadata() {
        Review review = approvedReview();
        review.setApproved(false);
        when(reviewRepository.findDetailedById(99L)).thenReturn(Optional.of(review));
        when(currentUserService.findCurrentUserId()).thenReturn(Optional.of("admin-1"));
        when(reviewRepository.save(review)).thenReturn(review);

        var response = reviewService.moderateReview(
                99L,
                ReviewModerateRequest.builder().approved(true).visible(false).build());

        assertThat(response.getApproved()).isTrue();
        assertThat(response.getVisible()).isFalse();
        assertThat(response.getModeratedBy()).isEqualTo("admin-1");
        assertThat(response.getModeratedAt()).isNotNull();
    }

    @Test
    void replyToReview_shouldSetAndClearAdminReply() {
        Review review = approvedReview();
        when(reviewRepository.findDetailedById(99L)).thenReturn(Optional.of(review));
        when(currentUserService.findCurrentUserId()).thenReturn(Optional.of("admin-1"));
        when(reviewRepository.save(review)).thenReturn(review);

        var replied = reviewService.replyToReview(
                99L, ReviewReplyRequest.builder().adminReply(" Thanks ").build());

        assertThat(replied.getAdminReply()).isEqualTo("Thanks");
        assertThat(replied.getRepliedBy()).isEqualTo("admin-1");
        assertThat(replied.getRepliedAt()).isNotNull();

        var cleared = reviewService.replyToReview(
                99L, ReviewReplyRequest.builder().adminReply(" ").build());

        assertThat(cleared.getAdminReply()).isNull();
        assertThat(cleared.getRepliedBy()).isNull();
    }

    private ReviewCreateRequest createRequest() {
        return ReviewCreateRequest.builder()
                .rating(5)
                .comment(" Great book ")
                .images(List.of(imageRequest("https://cdn.example.com/review.jpg", "review-img")))
                .build();
    }

    private ReviewUpdateRequest updateRequest() {
        return ReviewUpdateRequest.builder()
                .rating(4)
                .comment("Updated")
                .images(List.of(imageRequest("https://cdn.example.com/new.jpg", "review-new")))
                .build();
    }

    private ReviewImageRequest imageRequest(String url, String publicId) {
        return ReviewImageRequest.builder()
                .imageUrl(url)
                .imagePublicId(publicId)
                .sortOrder(0)
                .build();
    }

    private Review approvedReview() {
        Product product = product();
        ProductVariation variation = variation(product);
        Order order = order(OrderStatus.COMPLETED);
        Review review = Review.builder()
                .rating(5)
                .comment("Great")
                .approved(true)
                .visible(true)
                .user(order.getUser())
                .product(product)
                .productVariation(variation)
                .order(order)
                .orderItem(order.getItems().getFirst())
                .moderatedBy("admin")
                .moderatedAt(Instant.now())
                .build();
        review.setId(99L);
        review.getImages()
                .add(com.tien.aivirabackend.domain.entity.review.ReviewImage.builder()
                        .review(review)
                        .imageUrl("https://cdn.example.com/review.jpg")
                        .imagePublicId("review-img")
                        .sortOrder(0)
                        .build());
        return review;
    }

    private Order order(OrderStatus status) {
        User user = User.builder()
                .id("user-1")
                .username("buyer")
                .email("buyer@example.com")
                .build();
        Order order = Order.builder()
                .user(user)
                .orderCode("ORD123")
                .orderStatus(status)
                .build();
        order.setId(21L);
        order.getItems()
                .add(OrderItem.builder()
                        .order(order)
                        .productId(10L)
                        .productVariationId(11L)
                        .productName("Aivira Book")
                        .sku("BOOK-001")
                        .basePrice(BigDecimal.valueOf(100))
                        .finalPrice(BigDecimal.valueOf(100))
                        .quantity(1)
                        .build());
        order.getItems().getFirst().setId(31L);
        return order;
    }

    private Product product() {
        Product product =
                Product.builder().productName("Aivira Book").slug("aivira-book").build();
        product.setId(10L);
        return product;
    }

    private ProductVariation variation(Product product) {
        ProductVariation variation =
                ProductVariation.builder().product(product).sku("BOOK-001").build();
        variation.setId(11L);
        return variation;
    }
}
