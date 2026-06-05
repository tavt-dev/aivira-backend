package com.tien.aivirabackend.service.review;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.tien.aivirabackend.domain.entity.review.Review;

import jakarta.persistence.criteria.JoinType;

@Component
public class ReviewSpecifications {
    public Specification<Review> publicReviews(String productSlug, Integer rating) {
        return Specification.allOf(
                productSlug(productSlug),
                rating(rating),
                approved(true),
                visible(true),
                notDeleted());
    }

    public Specification<Review> adminReviews(
            Boolean approved,
            Boolean visible,
            Integer rating,
            String keyword,
            Long productId,
            String userId) {
        return Specification.allOf(
                approved(approved),
                visible(visible),
                rating(rating),
                productId(productId),
                userId(userId),
                keyword(keyword));
    }

    private Specification<Review> productSlug(String slug) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(slug)) {
                return null;
            }
            return cb.equal(root.join("product", JoinType.INNER).get("slug"), slug);
        };
    }

    private Specification<Review> productId(Long productId) {
        return (root, query, cb) -> productId == null ? null : cb.equal(root.get("product").get("id"), productId);
    }

    private Specification<Review> userId(String userId) {
        return (root, query, cb) ->
                !StringUtils.hasText(userId) ? null : cb.equal(root.get("user").get("id"), userId);
    }

    private Specification<Review> rating(Integer rating) {
        return (root, query, cb) -> rating == null ? null : cb.equal(root.get("rating"), rating);
    }

    private Specification<Review> approved(Boolean approved) {
        return (root, query, cb) -> approved == null ? null : cb.equal(root.get("approved"), approved);
    }

    private Specification<Review> visible(Boolean visible) {
        return (root, query, cb) -> visible == null ? null : cb.equal(root.get("visible"), visible);
    }

    private Specification<Review> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    private Specification<Review> keyword(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) {
                return null;
            }
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            var user = root.join("user", JoinType.LEFT);
            var product = root.join("product", JoinType.LEFT);
            var order = root.join("order", JoinType.LEFT);
            return cb.or(
                    cb.like(cb.lower(root.get("comment")), pattern),
                    cb.like(cb.lower(root.get("adminReply")), pattern),
                    cb.like(cb.lower(user.get("username")), pattern),
                    cb.like(cb.lower(user.get("email")), pattern),
                    cb.like(cb.lower(product.get("productName")), pattern),
                    cb.like(cb.lower(order.get("orderCode")), pattern));
        };
    }
}
