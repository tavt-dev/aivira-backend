package com.tien.aivirabackend.domain.mapper;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.tien.aivirabackend.domain.dto.response.CategoryResponse;
import com.tien.aivirabackend.domain.entity.catalog.Category;

@Component
public class CategoryMapper {
    public CategoryResponse toResponse(Category category) {
        if (category == null) {
            return null;
        }
        Category parent = category.getParentCategory();
        return CategoryResponse.builder().id(category.getId()).categoryName(category.getCategoryName())
                .slug(category.getSlug()).description(category.getDescription()).imageUrl(category.getImageUrl())
                .imagePublicId(category.getImagePublicId()).displayOrder(category.getDisplayOrder())
                .parentId(parent == null ? null : parent.getId()).active(category.getActive())
                .visible(category.getVisible()).createdAt(category.getCreatedAt()).updatedAt(category.getUpdatedAt())
                .build();
    }

    public CategoryResponse toTreeResponse(Category category) {
        CategoryResponse response = toResponse(category);
        if (response == null) {
            return null;
        }
        List<CategoryResponse> children = category.getChildCategories().stream()
                .filter(child -> Boolean.TRUE.equals(child.getActive()) && Boolean.TRUE.equals(child.getVisible()))
                .sorted(Comparator.comparing(Category::getDisplayOrder).thenComparing(Category::getCategoryName))
                .map(this::toTreeResponse).toList();
        response.setChildren(children);
        return response;
    }
}
