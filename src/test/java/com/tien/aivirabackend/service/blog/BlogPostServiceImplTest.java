package com.tien.aivirabackend.service.blog;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashSet;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tien.aivirabackend.config.properties.CloudinaryProperties;
import com.tien.aivirabackend.constant.BlogPostStatus;
import com.tien.aivirabackend.domain.entity.blog.BlogCategory;
import com.tien.aivirabackend.domain.entity.blog.BlogPost;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.mapper.BlogMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.BlogErrorCode;
import com.tien.aivirabackend.repository.*;
import com.tien.aivirabackend.service.auth.CurrentUserService;
import com.tien.aivirabackend.service.media.CloudinaryStorageService;
import com.tien.aivirabackend.service.media.FileValidatorService;

@ExtendWith(MockitoExtension.class)
class BlogPostServiceImplTest {
    @Mock
    BlogPostRepository postRepository;

    @Mock
    BlogCategoryRepository categoryRepository;

    @Mock
    BlogAssetRepository assetRepository;

    @Mock
    ProductRepository productRepository;

    @Mock
    CurrentUserService currentUserService;

    @Mock
    FileValidatorService fileValidatorService;

    @Mock
    CloudinaryStorageService cloudinaryStorageService;

    BlogPostServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BlogPostServiceImpl(postRepository, categoryRepository, assetRepository, productRepository,
                new BlogSpecifications(), new BlogHtmlSanitizer(), new BlogMapper(), currentUserService,
                fileValidatorService, cloudinaryStorageService, new CloudinaryProperties());
    }

    @Test
    void publishPost_whenReady_shouldPublishAndSetFirstPublishedAt() {
        BlogPost post = readyDraft();
        when(postRepository.findDetailedById(10L)).thenReturn(Optional.of(post));
        when(currentUserService.getCurrentUser()).thenReturn(post.getCreatedBy());
        when(postRepository.save(post)).thenReturn(post);

        var response = service.publishPost(10L);

        assertThat(response.getStatus()).isEqualTo(BlogPostStatus.PUBLISHED);
        assertThat(response.getPublishedAt()).isNotNull();
    }

    @Test
    void publishPost_withoutCover_shouldReject() {
        BlogPost post = readyDraft();
        post.setCoverUrl(null);
        when(postRepository.findDetailedById(10L)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> service.publishPost(10L)).isInstanceOf(AppException.class).extracting("errorCode")
                .isEqualTo(BlogErrorCode.BLOG_PUBLISH_VALIDATION_FAILED);
    }

    @Test
    void unpublishThenPublish_shouldKeepOriginalPublishedAt() {
        BlogPost post = readyDraft();
        java.time.Instant firstPublished = java.time.Instant.parse("2026-01-01T00:00:00Z");
        post.setStatus(BlogPostStatus.PUBLISHED);
        post.setPublishedAt(firstPublished);
        when(postRepository.findDetailedById(10L)).thenReturn(Optional.of(post));
        when(currentUserService.getCurrentUser()).thenReturn(post.getCreatedBy());
        when(postRepository.save(post)).thenReturn(post);

        service.unpublishPost(10L);
        service.publishPost(10L);

        assertThat(post.getPublishedAt()).isEqualTo(firstPublished);
    }

    private BlogPost readyDraft() {
        User admin = User.builder().id("admin-1").username("admin").build();
        BlogCategory category = BlogCategory.builder().id(1L).name("News").slug("news").active(true).build();
        return BlogPost.builder().id(10L).title("Aivira bookstore news").slug("aivira-bookstore-news")
                .excerpt("News excerpt").contentHtml("<p>Content</p>").coverUrl("https://cdn.example.com/cover.jpg")
                .category(category).createdBy(admin).updatedBy(admin).relatedProducts(new HashSet<>()).build();
    }
}
