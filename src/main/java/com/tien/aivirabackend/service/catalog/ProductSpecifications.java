package com.tien.aivirabackend.service.catalog;

import java.math.BigDecimal;
import java.util.Locale;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.tien.aivirabackend.constant.ProductStatus;
import com.tien.aivirabackend.domain.entity.catalog.Category;
import com.tien.aivirabackend.domain.entity.catalog.Product;

@Component
public class ProductSpecifications {
    public Specification<Product> publicProducts(String keyword, String categorySlug, String brand, String author,
            String publisher, String isbn, BigDecimal minPrice, BigDecimal maxPrice, Boolean available) {
        return (root, query, cb) -> {
            fetchDetails(root, query);
            Join<Product, Category> category = root.join("category");
            Predicate predicate = cb.conjunction();
            predicate = cb.and(predicate, cb.equal(root.get("status"), ProductStatus.ACTIVE));
            predicate = cb.and(predicate, cb.isTrue(root.get("active")));
            predicate = cb.and(predicate, cb.isTrue(category.get("active")), cb.isTrue(category.get("visible")));
            predicate = addCommonFilters(predicate, root, category, cb, keyword, categorySlug, brand);
            predicate = addBookFilters(predicate, root, cb, author, publisher, isbn);
            if (minPrice != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            if (available != null) {
                predicate = Boolean.TRUE.equals(available)
                        ? cb.and(predicate, cb.greaterThan(root.get("stockQuantity"), 0))
                        : cb.and(predicate, cb.lessThanOrEqualTo(root.get("stockQuantity"), 0));
            }
            return predicate;
        };
    }

    public Specification<Product> adminProducts(ProductStatus status, Long categoryId, String keyword) {
        return (root, query, cb) -> {
            fetchDetails(root, query);
            Predicate predicate = cb.conjunction();
            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            if (categoryId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("category").get("id"), categoryId));
            }
            if (StringUtils.hasText(keyword)) {
                predicate = cb.and(predicate, keywordPredicate(root, cb, keyword));
            }
            return predicate;
        };
    }

    private void fetchDetails(Root<Product> root, jakarta.persistence.criteria.CriteriaQuery<?> query) {
        if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
            root.fetch("category", JoinType.LEFT);
            root.fetch("productVariations", JoinType.LEFT);
            root.fetch("productMedia", JoinType.LEFT);
            query.distinct(true);
        }
    }

    private Predicate addCommonFilters(Predicate predicate, Root<Product> root,
            jakarta.persistence.criteria.Join<Product, Category> category,
            jakarta.persistence.criteria.CriteriaBuilder cb, String keyword, String categorySlug, String brand) {
        if (StringUtils.hasText(keyword)) {
            predicate = cb.and(predicate, keywordPredicate(root, cb, keyword));
        }
        if (StringUtils.hasText(categorySlug)) {
            predicate = cb.and(predicate, cb.equal(category.get("slug"), categorySlug.trim()));
        }
        if (StringUtils.hasText(brand)) {
            predicate = cb.and(predicate, cb.equal(cb.lower(root.get("brand")), brand.trim().toLowerCase(Locale.ROOT)));
        }
        return predicate;
    }

    private Predicate addBookFilters(Predicate predicate, Root<Product> root,
            jakarta.persistence.criteria.CriteriaBuilder cb, String author, String publisher, String isbn) {
        if (StringUtils.hasText(author)) {
            predicate = cb.and(predicate, containsIgnoreCase(root, cb, "bookAuthor", author));
        }
        if (StringUtils.hasText(publisher)) {
            predicate = cb.and(predicate, containsIgnoreCase(root, cb, "publisher", publisher));
        }
        if (StringUtils.hasText(isbn)) {
            predicate = cb.and(predicate, containsIgnoreCase(root, cb, "isbn", isbn));
        }
        return predicate;
    }

    private Predicate keywordPredicate(Root<Product> root, jakarta.persistence.criteria.CriteriaBuilder cb,
            String keyword) {
        String likeKeyword = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        return cb.or(cb.like(cb.lower(root.get("productName")), likeKeyword),
                cb.like(cb.lower(root.get("sku")), likeKeyword),
                cb.like(cb.lower(root.get("description")), likeKeyword),
                cb.like(cb.lower(root.get("brand")), likeKeyword),
                cb.like(cb.lower(root.get("bookAuthor")), likeKeyword),
                cb.like(cb.lower(root.get("publisher")), likeKeyword),
                cb.like(cb.lower(root.get("isbn")), likeKeyword));
    }

    private Predicate containsIgnoreCase(Root<Product> root, jakarta.persistence.criteria.CriteriaBuilder cb,
            String field, String value) {
        return cb.like(cb.lower(root.get(field)), "%" + value.trim().toLowerCase(Locale.ROOT) + "%");
    }
}
