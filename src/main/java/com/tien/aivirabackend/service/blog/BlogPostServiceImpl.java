package com.tien.aivirabackend.service.blog;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.tien.aivirabackend.config.properties.CloudinaryProperties;
import com.tien.aivirabackend.constant.BlogPostStatus;
import com.tien.aivirabackend.constant.MediaType;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.BlogPostCreateRequest;
import com.tien.aivirabackend.domain.dto.request.BlogPostUpdateRequest;
import com.tien.aivirabackend.domain.dto.response.BlogAssetResponse;
import com.tien.aivirabackend.domain.dto.response.BlogPostDetailResponse;
import com.tien.aivirabackend.domain.dto.response.BlogPostResponse;
import com.tien.aivirabackend.domain.dto.response.BlogPostSummaryResponse;
import com.tien.aivirabackend.domain.entity.blog.BlogAsset;
import com.tien.aivirabackend.domain.entity.blog.BlogCategory;
import com.tien.aivirabackend.domain.entity.blog.BlogPost;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.mapper.BlogMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.BlogErrorCode;
import com.tien.aivirabackend.repository.BlogAssetRepository;
import com.tien.aivirabackend.repository.BlogCategoryRepository;
import com.tien.aivirabackend.repository.BlogPostRepository;
import com.tien.aivirabackend.repository.ProductRepository;
import com.tien.aivirabackend.service.auth.CurrentUserService;
import com.tien.aivirabackend.service.media.CloudinaryStorageService;
import com.tien.aivirabackend.service.media.CloudinaryUploadResult;
import com.tien.aivirabackend.service.media.FileValidatorService;
import com.tien.aivirabackend.util.PageRequestUtils;
import com.tien.aivirabackend.util.SlugUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "BLOG-POST-SERVICE")
public class BlogPostServiceImpl implements BlogPostService {
    private final BlogPostRepository postRepository;
    private final BlogCategoryRepository categoryRepository;
    private final BlogAssetRepository assetRepository;
    private final ProductRepository productRepository;
    private final BlogSpecifications specifications;
    private final BlogHtmlSanitizer htmlSanitizer;
    private final BlogMapper blogMapper;
    private final CurrentUserService currentUserService;
    private final FileValidatorService fileValidatorService;
    private final CloudinaryStorageService cloudinaryStorageService;
    private final CloudinaryProperties cloudinaryProperties;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BlogPostSummaryResponse> getPublicPosts(
            String keyword, String categorySlug, String productSlug, String sort, int page, int size) {
        Sort pageSort =
                switch (sort == null ? "newest" : sort.toLowerCase()) {
                    case "oldest" -> Sort.by(Sort.Direction.ASC, "publishedAt");
                    case "title_asc" -> Sort.by(Sort.Direction.ASC, "title");
                    default -> Sort.by(Sort.Direction.DESC, "publishedAt");
                };
        Page<BlogPostSummaryResponse> result = postRepository
                .findAll(
                        specifications.publicPosts(keyword, categorySlug, productSlug),
                        PageRequestUtils.of(page, size, pageSort))
                .map(blogMapper::toSummaryResponse);
        return PageResponse.from(result);
    }

    @Override
    @Transactional(readOnly = true)
    public BlogPostDetailResponse getPublicPost(String slug) {
        BlogPost post = postRepository
                .findDetailedBySlug(slug)
                .filter(this::isPublic)
                .orElseThrow(() -> new AppException(BlogErrorCode.BLOG_POST_NOT_FOUND));
        return blogMapper.toDetailResponse(post);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BlogPostSummaryResponse> getAdminPosts(
            BlogPostStatus status,
            Long categoryId,
            String keyword,
            String createdBy,
            Instant publishedFrom,
            Instant publishedTo,
            int page,
            int size) {
        Page<BlogPostSummaryResponse> result = postRepository
                .findAll(
                        specifications.adminPosts(status, categoryId, keyword, createdBy, publishedFrom, publishedTo),
                        PageRequestUtils.newestFirst(page, size))
                .map(blogMapper::toSummaryResponse);
        return PageResponse.from(result);
    }

    @Override
    @Transactional(readOnly = true)
    public BlogPostResponse getAdminPost(Long id) {
        return blogMapper.toResponse(findActivePost(id));
    }

    @Override
    @Transactional
    public BlogPostResponse createPost(BlogPostCreateRequest request) {
        User admin = currentUserService.getCurrentUser();
        String slug = normalizeSlug(request.getSlug(), request.getTitle(), null);
        BlogPost post = BlogPost.builder()
                .title(request.getTitle().trim())
                .slug(slug)
                .excerpt(request.getExcerpt().trim())
                .contentHtml(htmlSanitizer.sanitize(request.getContentHtml()))
                .category(findCategory(request.getCategoryId()))
                .seoTitle(trimToNull(request.getSeoTitle()))
                .metaDescription(trimToNull(request.getMetaDescription()))
                .coverAltText(trimToNull(request.getCoverAltText()))
                .createdBy(admin)
                .updatedBy(admin)
                .relatedProducts(resolveProducts(request.getRelatedProductIds()))
                .build();
        return blogMapper.toResponse(postRepository.save(post));
    }

    @Override
    @Transactional
    public BlogPostResponse updatePost(Long id, BlogPostUpdateRequest request) {
        BlogPost post = findActivePost(id);
        post.setTitle(request.getTitle().trim());
        post.setSlug(normalizeSlug(request.getSlug(), request.getTitle(), id));
        post.setExcerpt(request.getExcerpt().trim());
        post.setContentHtml(htmlSanitizer.sanitize(request.getContentHtml()));
        post.setCategory(findCategory(request.getCategoryId()));
        post.setSeoTitle(trimToNull(request.getSeoTitle()));
        post.setMetaDescription(trimToNull(request.getMetaDescription()));
        post.setCoverAltText(trimToNull(request.getCoverAltText()));
        post.setUpdatedBy(currentUserService.getCurrentUser());
        post.getRelatedProducts().clear();
        post.getRelatedProducts().addAll(resolveProducts(request.getRelatedProductIds()));
        if (post.getStatus() == BlogPostStatus.PUBLISHED) {
            validatePublishable(post);
        }
        return blogMapper.toResponse(postRepository.save(post));
    }

    @Override
    @Transactional
    public BlogPostResponse publishPost(Long id) {
        BlogPost post = findActivePost(id);
        if (post.getStatus() == BlogPostStatus.PUBLISHED) {
            throw new AppException(BlogErrorCode.BLOG_INVALID_STATUS);
        }
        validatePublishable(post);
        post.setStatus(BlogPostStatus.PUBLISHED);
        if (post.getPublishedAt() == null) {
            post.setPublishedAt(Instant.now());
        }
        post.setUpdatedBy(currentUserService.getCurrentUser());
        return blogMapper.toResponse(postRepository.save(post));
    }

    @Override
    @Transactional
    public BlogPostResponse unpublishPost(Long id) {
        BlogPost post = findActivePost(id);
        if (post.getStatus() != BlogPostStatus.PUBLISHED) {
            throw new AppException(BlogErrorCode.BLOG_INVALID_STATUS);
        }
        post.setStatus(BlogPostStatus.DRAFT);
        post.setUpdatedBy(currentUserService.getCurrentUser());
        return blogMapper.toResponse(postRepository.save(post));
    }

    @Override
    @Transactional
    public void deletePost(Long id) {
        BlogPost post = findActivePost(id);
        post.setDeletedAt(Instant.now());
        post.setStatus(BlogPostStatus.DRAFT);
        post.setUpdatedBy(currentUserService.getCurrentUser());
        postRepository.save(post);
    }

    @Override
    @Transactional
    public BlogPostResponse uploadCover(Long id, MultipartFile file, String altText) {
        BlogPost post = findActivePost(id);
        fileValidatorService.validateFile(file, MediaType.IMAGE);
        CloudinaryUploadResult uploaded = cloudinaryStorageService.uploadBlogCover(
                file, cloudinaryProperties.getBlogImageFolder(), "post-" + id + "-cover");
        String previousPublicId = post.getCoverPublicId();
        post.setCoverUrl(uploaded.secureUrl());
        post.setCoverPublicId(uploaded.publicId());
        post.setCoverAltText(trimToNull(altText));
        post.setUpdatedBy(currentUserService.getCurrentUser());
        try {
            BlogPost saved = postRepository.saveAndFlush(post);
            cloudinaryStorageService.deleteImage(previousPublicId);
            return blogMapper.toResponse(saved);
        } catch (RuntimeException exception) {
            cloudinaryStorageService.deleteImage(uploaded.publicId());
            throw exception;
        }
    }

    @Override
    @Transactional
    public BlogPostResponse deleteCover(Long id) {
        BlogPost post = findActivePost(id);
        if (post.getStatus() == BlogPostStatus.PUBLISHED) {
            throw new AppException(BlogErrorCode.BLOG_PUBLISH_VALIDATION_FAILED);
        }
        String publicId = post.getCoverPublicId();
        post.setCoverUrl(null);
        post.setCoverPublicId(null);
        post.setCoverAltText(null);
        post.setUpdatedBy(currentUserService.getCurrentUser());
        BlogPost saved = postRepository.saveAndFlush(post);
        cloudinaryStorageService.deleteImage(publicId);
        return blogMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BlogAssetResponse uploadContentImage(Long id, MultipartFile file, String altText) {
        BlogPost post = findActivePost(id);
        fileValidatorService.validateFile(file, MediaType.IMAGE);
        CloudinaryUploadResult uploaded = cloudinaryStorageService.uploadBlogContentImage(
                file, cloudinaryProperties.getBlogImageFolder(), "post-" + id + "-content");
        try {
            BlogAsset asset = BlogAsset.builder()
                    .post(post)
                    .url(uploaded.secureUrl())
                    .publicId(uploaded.publicId())
                    .altText(trimToNull(altText))
                    .build();
            return blogMapper.toAssetResponse(assetRepository.saveAndFlush(asset));
        } catch (RuntimeException exception) {
            cloudinaryStorageService.deleteImage(uploaded.publicId());
            throw exception;
        }
    }

    @Override
    @Transactional
    public void deleteContentImage(Long postId, Long assetId) {
        BlogPost post = findActivePost(postId);
        BlogAsset asset = assetRepository
                .findByIdAndPost_Id(assetId, postId)
                .orElseThrow(() -> new AppException(BlogErrorCode.BLOG_ASSET_NOT_FOUND));
        if (post.getContentHtml() != null && post.getContentHtml().contains(asset.getUrl())) {
            throw new AppException(BlogErrorCode.BLOG_PUBLISH_VALIDATION_FAILED)
                    .addDetail("reason", "Remove the image from contentHtml before deleting the asset");
        }
        String publicId = asset.getPublicId();
        assetRepository.delete(asset);
        assetRepository.flush();
        cloudinaryStorageService.deleteImage(publicId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlogPostSummaryResponse> getLatestPosts(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        return postRepository
                .findAll(
                        specifications.publicPosts(null, null, null),
                        PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "publishedAt")))
                .stream()
                .map(blogMapper::toSummaryResponse)
                .toList();
    }

    private BlogPost findActivePost(Long id) {
        return postRepository
                .findDetailedById(id)
                .filter(post -> post.getDeletedAt() == null)
                .orElseThrow(() -> new AppException(BlogErrorCode.BLOG_POST_NOT_FOUND));
    }

    private BlogCategory findCategory(Long id) {
        return categoryRepository
                .findById(id)
                .orElseThrow(() -> new AppException(BlogErrorCode.BLOG_CATEGORY_NOT_FOUND));
    }

    private Set<Product> resolveProducts(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        List<Product> products = productRepository.findAllById(ids);
        if (products.size() != ids.size()) {
            throw new AppException(BlogErrorCode.BLOG_RELATED_PRODUCT_NOT_FOUND);
        }
        return new HashSet<>(products);
    }

    private String normalizeSlug(String requestedSlug, String title, Long currentId) {
        String slug = SlugUtils.slugify(requestedSlug, SlugUtils.slugify(title, "blog-post"));
        boolean duplicate = currentId == null
                ? postRepository.existsBySlug(slug)
                : postRepository.existsBySlugAndIdNot(slug, currentId);
        if (duplicate) {
            throw new AppException(BlogErrorCode.BLOG_SLUG_ALREADY_EXISTS);
        }
        return slug;
    }

    private void validatePublishable(BlogPost post) {
        if (!StringUtils.hasText(post.getTitle())
                || !StringUtils.hasText(post.getExcerpt())
                || !StringUtils.hasText(post.getContentHtml())
                || post.getCategory() == null
                || !Boolean.TRUE.equals(post.getCategory().getActive())
                || !StringUtils.hasText(post.getCoverUrl())) {
            throw new AppException(BlogErrorCode.BLOG_PUBLISH_VALIDATION_FAILED);
        }
    }

    private boolean isPublic(BlogPost post) {
        return post.getDeletedAt() == null
                && post.getStatus() == BlogPostStatus.PUBLISHED
                && Boolean.TRUE.equals(post.getCategory().getActive());
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
