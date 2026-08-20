package com.tien.aivirabackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.response.BlogPostDetailResponse;
import com.tien.aivirabackend.exception.GlobalExceptionHandler;
import com.tien.aivirabackend.service.blog.BlogCategoryService;
import com.tien.aivirabackend.service.blog.BlogPostService;

@ExtendWith(MockitoExtension.class)
class BlogControllerContractTest {
    @Mock
    BlogPostService postService;

    @Mock
    BlogCategoryService categoryService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BlogController(postService, categoryService))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void publicEndpoints_shouldNotRequireMethodAuthorization() throws Exception {
        assertThat(BlogController.class
                .getMethod("getPosts", String.class, String.class, String.class, String.class, int.class, int.class)
                .getAnnotation(PreAuthorize.class)).isNull();

        when(postService.getPublicPosts(null, null, null, "newest", 1, 20))
                .thenReturn(PageResponse.<com.tien.aivirabackend.domain.dto.response.BlogPostSummaryResponse> builder()
                        .data(List.of()).build());
        mockMvc.perform(get("/blog/posts")).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.data").isArray());
    }

    @Test
    void getPost_shouldReturnPublicDetailContract() throws Exception {
        when(postService.getPublicPost("book-news"))
                .thenReturn(BlogPostDetailResponse.builder().slug("book-news").relatedProducts(List.of()).build());

        mockMvc.perform(get("/blog/posts/book-news")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("book-news"))
                .andExpect(jsonPath("$.data.relatedProducts").isArray())
                .andExpect(jsonPath("$.data.coverPublicId").doesNotExist());
    }
}
