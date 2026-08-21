package com.tien.aivirabackend.service.catalog;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.tien.aivirabackend.domain.dto.request.CategoryRequest;
import com.tien.aivirabackend.domain.dto.response.CategoryResponse;
import com.tien.aivirabackend.domain.entity.catalog.Category;
import com.tien.aivirabackend.domain.mapper.CategoryMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.CategoryErrorCode;
import com.tien.aivirabackend.repository.CategoryRepository;
import com.tien.aivirabackend.util.SlugUtils;
import com.tien.aivirabackend.service.ai.CategoryCatalogChangedEvent;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryServiceImpl implements CategoryService {
    CategoryRepository categoryRepository;
    CategoryMapper categoryMapper;
    ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getVisibleCategories() {
        return categoryRepository
                .findAllByActiveTrueAndVisibleTrue(Sort.by("displayOrder").ascending().and(Sort.by("categoryName")))
                .stream().map(categoryMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getVisibleCategoryTree() {
        return categoryRepository
                .findAllByActiveTrueAndVisibleTrue(Sort.by("displayOrder").ascending().and(Sort.by("categoryName")))
                .stream().filter(category -> category.getParentCategory() == null).map(categoryMapper::toTreeResponse)
                .toList();
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String categoryName = request.getCategoryName().trim();
        if (categoryRepository.existsByCategoryNameIgnoreCase(categoryName)) {
            throw new AppException(CategoryErrorCode.CATEGORY_NAME_ALREADY_EXISTS);
        }

        String slug = resolveSlug(request.getSlug(), categoryName);
        if (categoryRepository.existsBySlug(slug)) {
            throw new AppException(CategoryErrorCode.CATEGORY_SLUG_ALREADY_EXISTS);
        }

        Category parent = request.getParentId() == null ? null : findCategory(request.getParentId());
        Category category = Category.builder().categoryName(categoryName).slug(slug)
                .description(request.getDescription().trim()).imageUrl(trimToNull(request.getImageUrl()))
                .imagePublicId(trimToNull(request.getImagePublicId()))
                .displayOrder(request.getDisplayOrder() == null ? 0 : request.getDisplayOrder()).parentCategory(parent)
                .active(request.getActive() == null ? true : request.getActive())
                .visible(request.getVisible() == null ? true : request.getVisible()).build();

        Category saved = categoryRepository.save(category);
        eventPublisher.publishEvent(new CategoryCatalogChangedEvent(saved.getId()));
        return categoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CategoryResponse update(Long categoryId, CategoryRequest request) {
        Category category = categoryRepository.findWithParentCategoryById(categoryId)
                .orElseThrow(() -> new AppException(CategoryErrorCode.CATEGORY_NOT_FOUND));

        String categoryName = request.getCategoryName().trim();
        if (categoryRepository.existsByCategoryNameIgnoreCaseAndIdNot(categoryName, categoryId)) {
            throw new AppException(CategoryErrorCode.CATEGORY_NAME_ALREADY_EXISTS);
        }

        String slug = resolveSlug(request.getSlug(), categoryName);
        if (categoryRepository.existsBySlugAndIdNot(slug, categoryId)) {
            throw new AppException(CategoryErrorCode.CATEGORY_SLUG_ALREADY_EXISTS);
        }

        Category parent = request.getParentId() == null ? null : findCategory(request.getParentId());
        validateParent(category, parent);

        category.setCategoryName(categoryName);
        category.setSlug(slug);
        category.setDescription(request.getDescription().trim());
        category.setImageUrl(trimToNull(request.getImageUrl()));
        category.setImagePublicId(trimToNull(request.getImagePublicId()));
        category.setDisplayOrder(request.getDisplayOrder() == null ? 0 : request.getDisplayOrder());
        category.setParentCategory(parent);
        category.setActive(request.getActive() == null ? category.getActive() : request.getActive());
        category.setVisible(request.getVisible() == null ? category.getVisible() : request.getVisible());

        Category saved = categoryRepository.save(category);
        eventPublisher.publishEvent(new CategoryCatalogChangedEvent(saved.getId()));
        return categoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long categoryId) {
        Category category = findCategory(categoryId);
        category.setActive(false);
        category.setVisible(false);
        categoryRepository.save(category);
        eventPublisher.publishEvent(new CategoryCatalogChangedEvent(categoryId));
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(CategoryErrorCode.CATEGORY_NOT_FOUND));
    }

    private void validateParent(Category category, Category parent) {
        if (parent == null) {
            return;
        }
        if (category.getId().equals(parent.getId())) {
            throw new AppException(CategoryErrorCode.CATEGORY_PARENT_INVALID);
        }

        Category current = parent;
        while (current != null) {
            if (category.getId().equals(current.getId())) {
                throw new AppException(CategoryErrorCode.CATEGORY_PARENT_CYCLE);
            }
            current = current.getParentCategory();
        }
    }

    private String resolveSlug(String requestedSlug, String fallbackName) {
        return SlugUtils.slugify(StringUtils.hasText(requestedSlug) ? requestedSlug : fallbackName, "category");
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
