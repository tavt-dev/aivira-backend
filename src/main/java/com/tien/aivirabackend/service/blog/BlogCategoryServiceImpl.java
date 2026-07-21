package com.tien.aivirabackend.service.blog;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tien.aivirabackend.domain.dto.request.BlogCategoryRequest;
import com.tien.aivirabackend.domain.dto.response.BlogCategoryResponse;
import com.tien.aivirabackend.domain.entity.blog.BlogCategory;
import com.tien.aivirabackend.domain.mapper.BlogMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.BlogErrorCode;
import com.tien.aivirabackend.repository.BlogCategoryRepository;
import com.tien.aivirabackend.repository.BlogPostRepository;
import com.tien.aivirabackend.util.SlugUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BlogCategoryServiceImpl implements BlogCategoryService {
    private final BlogCategoryRepository categoryRepository;
    private final BlogPostRepository postRepository;
    private final BlogMapper blogMapper;

    @Override
    @Transactional(readOnly = true)
    public java.util.List<BlogCategoryResponse> getPublicCategories() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                .map(blogMapper::toCategoryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<BlogCategoryResponse> getAdminCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .map(blogMapper::toCategoryResponse)
                .toList();
    }

    @Override
    @Transactional
    public BlogCategoryResponse createCategory(BlogCategoryRequest request) {
        String slug = SlugUtils.slugify(request.getSlug(), SlugUtils.slugify(request.getName(), "category"));
        if (categoryRepository.existsBySlug(slug)) {
            throw new AppException(BlogErrorCode.BLOG_SLUG_ALREADY_EXISTS);
        }
        BlogCategory category = BlogCategory.builder()
                .name(request.getName().trim())
                .slug(slug)
                .description(request.getDescription())
                .displayOrder(request.getDisplayOrder() == null ? 0 : request.getDisplayOrder())
                .active(request.getActive() == null || request.getActive())
                .build();
        return blogMapper.toCategoryResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public BlogCategoryResponse updateCategory(Long id, BlogCategoryRequest request) {
        BlogCategory category = findCategory(id);
        String slug = SlugUtils.slugify(request.getSlug(), SlugUtils.slugify(request.getName(), "category-" + id));
        if (categoryRepository.existsBySlugAndIdNot(slug, id)) {
            throw new AppException(BlogErrorCode.BLOG_SLUG_ALREADY_EXISTS);
        }
        category.setName(request.getName().trim());
        category.setSlug(slug);
        category.setDescription(request.getDescription());
        category.setDisplayOrder(request.getDisplayOrder() == null ? 0 : request.getDisplayOrder());
        category.setActive(request.getActive() == null ? category.getActive() : request.getActive());
        return blogMapper.toCategoryResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        BlogCategory category = findCategory(id);
        if (postRepository.existsByCategory_IdAndDeletedAtIsNull(id)) {
            throw new AppException(BlogErrorCode.BLOG_CATEGORY_IN_USE);
        }
        categoryRepository.delete(category);
    }

    private BlogCategory findCategory(Long id) {
        return categoryRepository
                .findById(id)
                .orElseThrow(() -> new AppException(BlogErrorCode.BLOG_CATEGORY_NOT_FOUND));
    }
}
