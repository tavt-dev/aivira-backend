package com.tien.aivirabackend.service.blog;

import java.time.Instant;

import org.springframework.web.multipart.MultipartFile;

import com.tien.aivirabackend.constant.BlogPostStatus;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.BlogPostCreateRequest;
import com.tien.aivirabackend.domain.dto.request.BlogPostUpdateRequest;
import com.tien.aivirabackend.domain.dto.response.BlogAssetResponse;
import com.tien.aivirabackend.domain.dto.response.BlogPostDetailResponse;
import com.tien.aivirabackend.domain.dto.response.BlogPostResponse;
import com.tien.aivirabackend.domain.dto.response.BlogPostSummaryResponse;

public interface BlogPostService {
    PageResponse<BlogPostSummaryResponse> getPublicPosts(
            String keyword, String categorySlug, String productSlug, String sort, int page, int size);

    BlogPostDetailResponse getPublicPost(String slug);

    PageResponse<BlogPostSummaryResponse> getAdminPosts(
            BlogPostStatus status,
            Long categoryId,
            String keyword,
            String createdBy,
            Instant publishedFrom,
            Instant publishedTo,
            int page,
            int size);

    BlogPostResponse getAdminPost(Long id);

    BlogPostResponse createPost(BlogPostCreateRequest request);

    BlogPostResponse updatePost(Long id, BlogPostUpdateRequest request);

    BlogPostResponse publishPost(Long id);

    BlogPostResponse unpublishPost(Long id);

    void deletePost(Long id);

    BlogPostResponse uploadCover(Long id, MultipartFile file, String altText);

    BlogPostResponse deleteCover(Long id);

    BlogAssetResponse uploadContentImage(Long id, MultipartFile file, String altText);

    void deleteContentImage(Long postId, Long assetId);

    java.util.List<BlogPostSummaryResponse> getLatestPosts(int limit);
}
