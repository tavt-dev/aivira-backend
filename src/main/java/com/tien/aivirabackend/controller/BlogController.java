package com.tien.aivirabackend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.response.BlogCategoryResponse;
import com.tien.aivirabackend.domain.dto.response.BlogPostDetailResponse;
import com.tien.aivirabackend.domain.dto.response.BlogPostSummaryResponse;
import com.tien.aivirabackend.service.blog.BlogCategoryService;
import com.tien.aivirabackend.service.blog.BlogPostService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/blog")
@Tag(name = "Public Blog")
@RequiredArgsConstructor
public class BlogController {
    private final BlogPostService postService;
    private final BlogCategoryService categoryService;

    @GetMapping("/posts")
    @Operation(summary = "List published blog posts")
    public ResponseEntity<ApiResponse<PageResponse<BlogPostSummaryResponse>>> getPosts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) String productSlug,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                "Get blog posts successful",
                postService.getPublicPosts(keyword, categorySlug, productSlug, sort, page, size)));
    }

    @GetMapping("/posts/{slug}")
    @Operation(summary = "Get a published blog post")
    public ResponseEntity<ApiResponse<BlogPostDetailResponse>> getPost(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success("Get blog post successful", postService.getPublicPost(slug)));
    }

    @GetMapping("/categories")
    @Operation(summary = "List active blog categories")
    public ResponseEntity<ApiResponse<List<BlogCategoryResponse>>> getCategories() {
        return ResponseEntity.ok(
                ApiResponse.success("Get blog categories successful", categoryService.getPublicCategories()));
    }
}
