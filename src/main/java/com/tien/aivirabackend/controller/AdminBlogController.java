package com.tien.aivirabackend.controller;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.tien.aivirabackend.constant.BlogPostStatus;
import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.BlogCategoryRequest;
import com.tien.aivirabackend.domain.dto.request.BlogPostCreateRequest;
import com.tien.aivirabackend.domain.dto.request.BlogPostUpdateRequest;
import com.tien.aivirabackend.domain.dto.response.BlogAssetResponse;
import com.tien.aivirabackend.domain.dto.response.BlogCategoryResponse;
import com.tien.aivirabackend.domain.dto.response.BlogPostResponse;
import com.tien.aivirabackend.domain.dto.response.BlogPostSummaryResponse;
import com.tien.aivirabackend.service.blog.BlogCategoryService;
import com.tien.aivirabackend.service.blog.BlogPostService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/blog")
@Tag(name = "Admin Blog")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AdminBlogController {
    private final BlogPostService postService;
    private final BlogCategoryService categoryService;

    @GetMapping("/categories")
    @PreAuthorize("@authorizationService.hasAnyPermission('CMS_READ', 'CMS_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<List<BlogCategoryResponse>>> getCategories() {
        return ResponseEntity
                .ok(ApiResponse.success("Get admin blog categories successful", categoryService.getAdminCategories()));
    }

    @PostMapping("/categories")
    @PreAuthorize("@authorizationService.hasAnyPermission('CMS_CREATE', 'CMS_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<BlogCategoryResponse>> createCategory(
            @Valid @RequestBody BlogCategoryRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("Create blog category successful", categoryService.createCategory(request)));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("@authorizationService.hasAnyPermission('CMS_UPDATE', 'CMS_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<BlogCategoryResponse>> updateCategory(@PathVariable Long id,
            @Valid @RequestBody BlogCategoryRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Update blog category successful", categoryService.updateCategory(id, request)));
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("@authorizationService.hasAnyPermission('CMS_DELETE', 'CMS_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Delete blog category successful", null));
    }

    @GetMapping("/posts")
    @PreAuthorize("@authorizationService.hasAnyPermission('CMS_READ', 'CMS_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<PageResponse<BlogPostSummaryResponse>>> getPosts(
            @RequestParam(required = false) BlogPostStatus status, @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword, @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) Instant publishedFrom, @RequestParam(required = false) Instant publishedTo,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("Get admin blog posts successful", postService
                .getAdminPosts(status, categoryId, keyword, createdBy, publishedFrom, publishedTo, page, size)));
    }

    @GetMapping("/posts/{id}")
    @PreAuthorize("@authorizationService.hasAnyPermission('CMS_READ', 'CMS_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<BlogPostResponse>> getPost(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Get admin blog post successful", postService.getAdminPost(id)));
    }

    @PostMapping("/posts")
    @PreAuthorize("@authorizationService.hasAnyPermission('CMS_CREATE', 'CMS_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<BlogPostResponse>> createPost(@Valid @RequestBody BlogPostCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Create blog post successful", postService.createPost(request)));
    }

    @PutMapping("/posts/{id}")
    @PreAuthorize("@authorizationService.hasAnyPermission('CMS_UPDATE', 'CMS_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<BlogPostResponse>> updatePost(@PathVariable Long id,
            @Valid @RequestBody BlogPostUpdateRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("Update blog post successful", postService.updatePost(id, request)));
    }

    @PutMapping("/posts/{id}/publish")
    @PreAuthorize("@authorizationService.hasAnyPermission('CMS_UPDATE', 'CMS_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<BlogPostResponse>> publishPost(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Publish blog post successful", postService.publishPost(id)));
    }

    @PutMapping("/posts/{id}/unpublish")
    @PreAuthorize("@authorizationService.hasAnyPermission('CMS_UPDATE', 'CMS_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<BlogPostResponse>> unpublishPost(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Unpublish blog post successful", postService.unpublishPost(id)));
    }

    @DeleteMapping("/posts/{id}")
    @PreAuthorize("@authorizationService.hasAnyPermission('CMS_DELETE', 'CMS_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.ok(ApiResponse.success("Delete blog post successful", null));
    }

    @PostMapping(value = "/posts/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload or replace blog cover")
    @PreAuthorize("@authorizationService.hasAnyPermission('CMS_CREATE', 'CMS_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<BlogPostResponse>> uploadCover(@PathVariable Long id,
            @RequestPart("file") MultipartFile file, @RequestParam(required = false) String altText) {
        return ResponseEntity
                .ok(ApiResponse.success("Upload blog cover successful", postService.uploadCover(id, file, altText)));
    }

    @DeleteMapping("/posts/{id}/cover")
    @PreAuthorize("@authorizationService.hasAnyPermission('CMS_UPDATE', 'CMS_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<BlogPostResponse>> deleteCover(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Delete blog cover successful", postService.deleteCover(id)));
    }

    @PostMapping(value = "/posts/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a rich-text blog image")
    @PreAuthorize("@authorizationService.hasAnyPermission('CMS_CREATE', 'CMS_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<BlogAssetResponse>> uploadContentImage(@PathVariable Long id,
            @RequestPart("file") MultipartFile file, @RequestParam(required = false) String altText) {
        return ResponseEntity.ok(
                ApiResponse.success("Upload blog image successful", postService.uploadContentImage(id, file, altText)));
    }

    @DeleteMapping("/posts/{postId}/images/{assetId}")
    @PreAuthorize("@authorizationService.hasAnyPermission('CMS_UPDATE', 'CMS_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<Void>> deleteContentImage(@PathVariable Long postId, @PathVariable Long assetId) {
        postService.deleteContentImage(postId, assetId);
        return ResponseEntity.ok(ApiResponse.success("Delete blog image successful", null));
    }
}
