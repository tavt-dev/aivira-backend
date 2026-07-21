package com.tien.aivirabackend.service.blog;

import java.time.Instant;

import jakarta.persistence.criteria.JoinType;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.tien.aivirabackend.constant.BlogPostStatus;
import com.tien.aivirabackend.domain.entity.blog.BlogPost;

@Component
public class BlogSpecifications {
    public Specification<BlogPost> publicPosts(String keyword, String categorySlug, String productSlug) {
        return Specification.allOf(
                status(BlogPostStatus.PUBLISHED),
                notDeleted(),
                activeCategory(),
                keyword(keyword),
                categorySlug(categorySlug),
                productSlug(productSlug));
    }

    public Specification<BlogPost> adminPosts(
            BlogPostStatus status,
            Long categoryId,
            String keyword,
            String createdBy,
            Instant publishedFrom,
            Instant publishedTo) {
        return Specification.allOf(
                notDeleted(),
                status(status),
                categoryId(categoryId),
                keyword(keyword),
                createdBy(createdBy),
                publishedFrom(publishedFrom),
                publishedTo(publishedTo));
    }

    private Specification<BlogPost> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    private Specification<BlogPost> activeCategory() {
        return (root, query, cb) ->
                cb.isTrue(root.join("category", JoinType.INNER).get("active"));
    }

    private Specification<BlogPost> status(BlogPostStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    private Specification<BlogPost> categoryId(Long categoryId) {
        return (root, query, cb) ->
                categoryId == null ? null : cb.equal(root.get("category").get("id"), categoryId);
    }

    private Specification<BlogPost> categorySlug(String categorySlug) {
        return (root, query, cb) -> !StringUtils.hasText(categorySlug)
                ? null
                : cb.equal(root.get("category").get("slug"), categorySlug.trim().toLowerCase());
    }

    private Specification<BlogPost> productSlug(String productSlug) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(productSlug)) {
                return null;
            }
            query.distinct(true);
            return cb.equal(
                    root.join("relatedProducts", JoinType.INNER).get("slug"),
                    productSlug.trim().toLowerCase());
        };
    }

    private Specification<BlogPost> createdBy(String userId) {
        return (root, query, cb) -> !StringUtils.hasText(userId)
                ? null
                : cb.equal(root.get("createdBy").get("id"), userId);
    }

    private Specification<BlogPost> publishedFrom(Instant from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("publishedAt"), from);
    }

    private Specification<BlogPost> publishedTo(Instant to) {
        return (root, query, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("publishedAt"), to);
    }

    private Specification<BlogPost> keyword(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) {
                return null;
            }
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("excerpt")), pattern),
                    cb.like(cb.lower(root.get("contentHtml")), pattern));
        };
    }
}
