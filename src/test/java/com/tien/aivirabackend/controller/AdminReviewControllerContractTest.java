package com.tien.aivirabackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.tien.aivirabackend.domain.dto.request.ReviewModerateRequest;
import com.tien.aivirabackend.domain.dto.request.ReviewReplyRequest;
import com.tien.aivirabackend.exception.GlobalExceptionHandler;
import com.tien.aivirabackend.service.review.ReviewService;

@ExtendWith(MockitoExtension.class)
class AdminReviewControllerContractTest {
    @Mock
    ReviewService reviewService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminReviewController(reviewService))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void endpoints_shouldDeclareExpectedPermissions() throws Exception {
        assertPreAuthorize(
                AdminReviewController.class.getMethod("getAdminReviews", Boolean.class, Boolean.class, Integer.class,
                        String.class, Long.class, String.class, int.class, int.class),
                "REVIEW_MANAGE_ALL", "REVIEW_READ_ALL");
        assertPreAuthorize(
                AdminReviewController.class.getMethod("moderateReview", Long.class, ReviewModerateRequest.class),
                "REVIEW_MANAGE_ALL", "REVIEW_MODERATE");
        assertPreAuthorize(AdminReviewController.class.getMethod("replyToReview", Long.class, ReviewReplyRequest.class),
                "REVIEW_MANAGE_ALL", "REVIEW_MODERATE");
    }

    @Test
    void endpoints_shouldDelegateToService() throws Exception {
        mockMvc.perform(get("/admin/reviews").param("approved", "true").param("visible", "true").param("rating", "5")
                .param("keyword", "book").param("productId", "10").param("userId", "user-1").param("page", "2")
                .param("size", "10")).andExpect(status().isOk());
        mockMvc.perform(put("/admin/reviews/99/moderate").contentType(MediaType.APPLICATION_JSON)
                .content("{\"approved\":true,\"visible\":false}")).andExpect(status().isOk());
        mockMvc.perform(put("/admin/reviews/99/reply").contentType(MediaType.APPLICATION_JSON)
                .content("{\"adminReply\":\"Thanks\"}")).andExpect(status().isOk());

        verify(reviewService).getAdminReviews(true, true, 5, "book", 10L, "user-1", 2, 10);
        verify(reviewService).moderateReview(eq(99L), any(ReviewModerateRequest.class));
        verify(reviewService).replyToReview(eq(99L), any(ReviewReplyRequest.class));
    }

    @Test
    void moderateReview_whenPayloadInvalid_shouldReturnValidationError() throws Exception {
        mockMvc.perform(put("/admin/reviews/99/moderate").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    private void assertPreAuthorize(Method method, String... permissions) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        for (String permission : permissions) {
            assertThat(preAuthorize.value()).contains(permission);
        }
    }
}
