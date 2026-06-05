package com.tien.aivirabackend.service.storefront;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tien.aivirabackend.domain.dto.response.CategoryHighlightResponse;
import com.tien.aivirabackend.domain.dto.response.StorefrontHomeResponse;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.mapper.ProductMapper;
import com.tien.aivirabackend.repository.CategoryRepository;
import com.tien.aivirabackend.repository.ProductRepository;
import com.tien.aivirabackend.repository.projection.CategoryHighlightProjection;
import com.tien.aivirabackend.service.catalog.ProductSpecifications;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StorefrontServiceImpl implements StorefrontService {
    static final int BOOK_SECTION_LIMIT = 8;
    static final int CATEGORY_HIGHLIGHT_LIMIT = 6;

    ProductRepository productRepository;
    CategoryRepository categoryRepository;
    ProductSpecifications productSpecifications;
    ProductMapper productMapper;

    @Override
    @Transactional(readOnly = true)
    public StorefrontHomeResponse getHome() {
        return StorefrontHomeResponse.builder()
                .featuredBooks(findBooks(featuredSpec(), Sort.by(Sort.Direction.DESC, "createdAt")))
                .newArrivals(findBooks(publicBookSpec(), Sort.by(Sort.Direction.DESC, "createdAt")))
                .bestsellingBooks(findBooks(
                        publicBookSpec(),
                        Sort.by(Sort.Direction.DESC, "soldCount").and(Sort.by(Sort.Direction.DESC, "createdAt"))))
                .categoryHighlights(categoryRepository
                        .findCategoryHighlights(PageRequest.of(0, CATEGORY_HIGHLIGHT_LIMIT))
                        .stream()
                        .map(this::toCategoryHighlight)
                        .toList())
                .build();
    }

    private java.util.List<com.tien.aivirabackend.domain.dto.response.ProductResponse> findBooks(
            Specification<Product> specification, Sort sort) {
        return productRepository.findAll(specification, PageRequest.of(0, BOOK_SECTION_LIMIT, sort)).stream()
                .map(productMapper::toResponse)
                .toList();
    }

    private Specification<Product> publicBookSpec() {
        return productSpecifications.publicProducts(null, null, null, null, null, null, null, null, true);
    }

    private Specification<Product> featuredSpec() {
        return publicBookSpec().and((root, query, cb) -> cb.isTrue(root.get("featured")));
    }

    private CategoryHighlightResponse toCategoryHighlight(CategoryHighlightProjection projection) {
        return CategoryHighlightResponse.builder()
                .categoryId(projection.getCategoryId())
                .categoryName(projection.getCategoryName())
                .slug(projection.getSlug())
                .description(projection.getDescription())
                .imageUrl(projection.getImageUrl())
                .imagePublicId(projection.getImagePublicId())
                .displayOrder(projection.getDisplayOrder())
                .bookCount(projection.getBookCount() == null ? 0L : projection.getBookCount())
                .build();
    }
}
