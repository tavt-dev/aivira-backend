package com.tien.aivirabackend.service.catalog;

import java.util.List;

import com.tien.aivirabackend.domain.dto.request.CategoryRequest;
import com.tien.aivirabackend.domain.dto.response.CategoryResponse;

public interface CategoryService {
    List<CategoryResponse> getVisibleCategories();

    List<CategoryResponse> getVisibleCategoryTree();

    CategoryResponse create(CategoryRequest request);

    CategoryResponse update(Long categoryId, CategoryRequest request);

    void delete(Long categoryId);
}
