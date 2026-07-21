package com.tien.aivirabackend.service.blog;

import java.util.List;

import com.tien.aivirabackend.domain.dto.request.BlogCategoryRequest;
import com.tien.aivirabackend.domain.dto.response.BlogCategoryResponse;

public interface BlogCategoryService {
    List<BlogCategoryResponse> getPublicCategories();

    List<BlogCategoryResponse> getAdminCategories();

    BlogCategoryResponse createCategory(BlogCategoryRequest request);

    BlogCategoryResponse updateCategory(Long id, BlogCategoryRequest request);

    void deleteCategory(Long id);
}
