package com.tien.aivirabackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.tien.aivirabackend.domain.dto.request.ReviewCreateRequest;
import com.tien.aivirabackend.domain.dto.request.ReviewUpdateRequest;
import com.tien.aivirabackend.exception.GlobalExceptionHandler;
import com.tien.aivirabackend.service.review.ReviewService;

@ExtendWith(MockitoExtension.class)
class ReviewControllerContractTest {
    @Mock
    ReviewService reviewService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ReviewController(reviewService))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void customerEndpoints_shouldDeclareExpectedPermissions() throws Exception {
        assertPreAuthorize(
                ReviewController.class.getMethod("createReview", Long.class, Long.class, ReviewCreateRequest.class),
                "REVIEW_CREATE_SELF");
        assertPreAuthorize(ReviewController.class.getMethod("createReviewWithImages", Long.class, Long.class,
                ReviewCreateRequest.class, java.util.List.class), "REVIEW_CREATE_SELF");
        assertPreAuthorize(ReviewController.class.getMethod("updateReview", Long.class, ReviewUpdateRequest.class),
                "REVIEW_UPDATE_SELF");
        assertPreAuthorize(ReviewController.class.getMethod("deleteReview", Long.class), "REVIEW_DELETE_SELF");
    }

    @Test
    void endpoints_shouldDelegateToService() throws Exception {
        mockMvc.perform(get("/products/aivira-book/reviews").param("rating", "5").param("sort", "rating_desc")
                .param("page", "2").param("size", "10")).andExpect(status().isOk());
        mockMvc.perform(
                post("/orders/21/items/31/review").contentType(MediaType.APPLICATION_JSON).content(validReviewJson()))
                .andExpect(status().isOk());
        mockMvc.perform(put("/reviews/99").contentType(MediaType.APPLICATION_JSON).content(validReviewJson()))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/reviews/99")).andExpect(status().isOk());

        verify(reviewService).getPublicReviews("aivira-book", 5, "rating_desc", 2, 10);
        verify(reviewService).createReview(eq(21L), eq(31L), any(ReviewCreateRequest.class));
        verify(reviewService).updateReview(eq(99L), any(ReviewUpdateRequest.class));
        verify(reviewService).deleteReview(99L);
    }

    @Test
    void createReviewWithImages_shouldAcceptMultipartWithAndWithoutImages() throws Exception {
        MockMultipartFile reviewPart = new MockMultipartFile("review", "review.json", MediaType.APPLICATION_JSON_VALUE,
                validReviewWithoutImagesJson().getBytes());
        MockMultipartFile image = new MockMultipartFile("images", "book.jpg", MediaType.IMAGE_JPEG_VALUE,
                new byte[] { 1, 2, 3 });

        mockMvc.perform(multipart("/orders/21/items/31/review").file(reviewPart).file(image))
                .andExpect(status().isOk());
        mockMvc.perform(multipart("/orders/21/items/31/review").file(reviewPart)).andExpect(status().isOk());

        verify(reviewService).createReviewWithImages(eq(21L), eq(31L), any(ReviewCreateRequest.class),
                argThat(files -> files != null && files.size() == 1));
        verify(reviewService).createReviewWithImages(eq(21L), eq(31L), any(ReviewCreateRequest.class), isNull());
    }

    @Test
    void createReview_whenPayloadInvalid_shouldReturnValidationError() throws Exception {
        mockMvc.perform(post("/orders/21/items/31/review").contentType(MediaType.APPLICATION_JSON).content("""
                {
                "rating":6,
                "comment":"ok",
                "images":[{"imageUrl":"","imagePublicId":"","sortOrder":0}]
                }
                """)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
    }

    private String validReviewJson() {
        return """
                {
                "rating":5,
                "comment":"Great book",
                "images":[{"imageUrl":"https://cdn.example.com/review.jpg","imagePublicId":"review-img","sortOrder":0}]
                }
                """;
    }

    private String validReviewWithoutImagesJson() {
        return """
                {
                "rating":5,
                "comment":"Great book",
                "images":[]
                }
                """;
    }

    private void assertPreAuthorize(Method method, String permission) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).contains(permission);
    }
}
