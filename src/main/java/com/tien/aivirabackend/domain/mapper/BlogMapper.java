package com.tien.aivirabackend.domain.mapper;

import java.util.Comparator;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.tien.aivirabackend.domain.dto.response.*;
import com.tien.aivirabackend.domain.entity.blog.BlogAsset;
import com.tien.aivirabackend.domain.entity.blog.BlogCategory;
import com.tien.aivirabackend.domain.entity.blog.BlogPost;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.entity.user.User;

@Component
public class BlogMapper {
    public BlogCategoryResponse toCategoryResponse(BlogCategory category) {
        return BlogCategoryResponse.builder().id(category.getId()).name(category.getName()).slug(category.getSlug())
                .description(category.getDescription()).displayOrder(category.getDisplayOrder())
                .active(category.getActive()).createdAt(category.getCreatedAt()).updatedAt(category.getUpdatedAt())
                .build();
    }

    public BlogPostSummaryResponse toSummaryResponse(BlogPost post) {
        return BlogPostSummaryResponse.builder().id(post.getId()).title(post.getTitle()).slug(post.getSlug())
                .excerpt(post.getExcerpt()).status(post.getStatus()).coverUrl(post.getCoverUrl())
                .coverAltText(post.getCoverAltText()).category(toCategoryResponse(post.getCategory()))
                .author(toAuthorResponse(post.getCreatedBy())).publishedAt(post.getPublishedAt())
                .createdAt(post.getCreatedAt()).updatedAt(post.getUpdatedAt()).build();
    }

    public BlogPostResponse toResponse(BlogPost post) {
        return BlogPostResponse.builder().id(post.getId()).title(post.getTitle()).slug(post.getSlug())
                .excerpt(post.getExcerpt()).contentHtml(post.getContentHtml()).status(post.getStatus())
                .coverUrl(post.getCoverUrl()).coverPublicId(post.getCoverPublicId())
                .coverAltText(post.getCoverAltText()).seoTitle(post.getSeoTitle())
                .metaDescription(post.getMetaDescription()).category(toCategoryResponse(post.getCategory()))
                .author(toAuthorResponse(post.getCreatedBy()))
                .updatedBy(post.getUpdatedBy() == null ? null : post.getUpdatedBy().getId())
                .publishedAt(post.getPublishedAt()).deletedAt(post.getDeletedAt()).createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .relatedProducts(
                        post.getRelatedProducts().stream().sorted(Comparator.comparing(Product::getProductName))
                                .map(this::toRelatedProductResponse).toList())
                .assets(post.getAssets().stream().sorted(Comparator.comparing(BlogAsset::getId))
                        .map(this::toAssetResponse).toList())
                .build();
    }

    public BlogPostDetailResponse toDetailResponse(BlogPost post) {
        return BlogPostDetailResponse.builder().id(post.getId()).title(post.getTitle()).slug(post.getSlug())
                .excerpt(post.getExcerpt()).contentHtml(post.getContentHtml()).coverUrl(post.getCoverUrl())
                .coverAltText(post.getCoverAltText()).seoTitle(post.getSeoTitle())
                .metaDescription(post.getMetaDescription()).category(toCategoryResponse(post.getCategory()))
                .author(toAuthorResponse(post.getCreatedBy())).publishedAt(post.getPublishedAt())
                .updatedAt(post.getUpdatedAt())
                .relatedProducts(
                        post.getRelatedProducts().stream().sorted(Comparator.comparing(Product::getProductName))
                                .map(this::toRelatedProductResponse).toList())
                .build();
    }

    public BlogAssetResponse toAssetResponse(BlogAsset asset) {
        return BlogAssetResponse.builder().id(asset.getId()).url(asset.getUrl()).publicId(asset.getPublicId())
                .altText(asset.getAltText()).createdAt(asset.getCreatedAt()).build();
    }

    private BlogAuthorResponse toAuthorResponse(User user) {
        String fullName = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return BlogAuthorResponse.builder().id(user.getId())
                .displayName(StringUtils.hasText(fullName) ? fullName : user.getUsername())
                .avatarUrl(user.getAvatarUrl()).build();
    }

    private BlogRelatedProductResponse toRelatedProductResponse(Product product) {
        return BlogRelatedProductResponse.builder().id(product.getId()).productName(product.getProductName())
                .slug(product.getSlug()).bookAuthor(product.getBookAuthor()).thumbnailUrl(product.getThumbnailUrl())
                .build();
    }
}
