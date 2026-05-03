package com.tien.aivirabackend.service.product;

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
import com.tien.aivirabackend.constant.ShopStatus;
import com.tien.aivirabackend.domain.entity.catalog.Category;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.entity.marketplace.Shop;

@Component
public class ProductSpecifications {
    public Specification<Product> publicProducts(
            String keyword,
            String categorySlug,
            String shopSlug,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean available) {
        return (root, query, cb) -> {
            fetchDetails(root, query);
            Join<Product, Category> category = root.join("category");
            Join<Product, Shop> shop = root.join("shop");
            Predicate predicate = cb.conjunction();
            predicate = cb.and(predicate, cb.equal(root.get("status"), ProductStatus.ACTIVE));
            predicate = cb.and(predicate, cb.isTrue(root.get("active")));
            predicate = cb.and(predicate, cb.isTrue(category.get("active")), cb.isTrue(category.get("visible")));
            predicate = cb.and(predicate, cb.equal(shop.get("status"), ShopStatus.APPROVED));
            predicate = addCommonFilters(predicate, root, category, shop, cb, keyword, categorySlug, shopSlug, brand);
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

    public Specification<Product> sellerProducts(Long shopId, ProductStatus status, String keyword) {
        return (root, query, cb) -> {
            fetchDetails(root, query);
            Predicate predicate = cb.equal(root.get("shop").get("id"), shopId);
            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(keyword)) {
                predicate = cb.and(predicate, keywordPredicate(root, cb, keyword));
            }
            return predicate;
        };
    }

    public Specification<Product> adminProducts(ProductStatus status, Long shopId, Long categoryId, String keyword) {
        return (root, query, cb) -> {
            fetchDetails(root, query);
            Predicate predicate = cb.conjunction();
            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            if (shopId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("shop").get("id"), shopId));
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
            root.fetch("shop", JoinType.LEFT);
            root.fetch("category", JoinType.LEFT);
            root.fetch("productVariations", JoinType.LEFT);
            root.fetch("productMedia", JoinType.LEFT);
            query.distinct(true);
        }
    }

    private Predicate addCommonFilters(
            Predicate predicate,
            Root<Product> root,
            Join<Product, Category> category,
            Join<Product, Shop> shop,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            String keyword,
            String categorySlug,
            String shopSlug,
            String brand) {
        if (StringUtils.hasText(keyword)) {
            predicate = cb.and(predicate, keywordPredicate(root, cb, keyword));
        }
        if (StringUtils.hasText(categorySlug)) {
            predicate = cb.and(predicate, cb.equal(category.get("slug"), categorySlug.trim()));
        }
        if (StringUtils.hasText(shopSlug)) {
            predicate = cb.and(predicate, cb.equal(shop.get("slug"), shopSlug.trim()));
        }
        if (StringUtils.hasText(brand)) {
            predicate = cb.and(
                    predicate,
                    cb.equal(cb.lower(root.get("brand")), brand.trim().toLowerCase(Locale.ROOT)));
        }
        return predicate;
    }

    private Predicate keywordPredicate(
            Root<Product> root, jakarta.persistence.criteria.CriteriaBuilder cb, String keyword) {
        String likeKeyword = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        return cb.or(
                cb.like(cb.lower(root.get("productName")), likeKeyword),
                cb.like(cb.lower(root.get("sku")), likeKeyword),
                cb.like(cb.lower(root.get("description")), likeKeyword),
                cb.like(cb.lower(root.get("brand")), likeKeyword));
    }
}
