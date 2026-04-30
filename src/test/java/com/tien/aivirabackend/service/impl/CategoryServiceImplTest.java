package com.tien.aivirabackend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tien.aivirabackend.domain.dto.request.CategoryRequest;
import com.tien.aivirabackend.domain.dto.response.CategoryResponse;
import com.tien.aivirabackend.domain.entity.catalog.Category;
import com.tien.aivirabackend.domain.mapper.CategoryMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.repository.CategoryRepository;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {
    @Mock
    CategoryRepository categoryRepository;

    @Mock
    CategoryMapper categoryMapper;

    @InjectMocks
    CategoryServiceImpl categoryService;

    @Test
    void create_shouldCreateVisibleCategoryWithGeneratedSlug() {
        CategoryRequest request = CategoryRequest.builder()
                .categoryName("Women's Fashion")
                .description("Fashion")
                .build();
        CategoryResponse response =
                CategoryResponse.builder().id(1L).slug("women-s-fashion").build();

        when(categoryRepository.existsByCategoryNameIgnoreCase("Women's Fashion"))
                .thenReturn(false);
        when(categoryRepository.existsBySlug("women-s-fashion")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(1L);
            return category;
        });
        when(categoryMapper.toResponse(any(Category.class))).thenReturn(response);

        CategoryResponse result = categoryService.create(request);

        assertThat(result).isSameAs(response);
        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());
        assertThat(categoryCaptor.getValue().getSlug()).isEqualTo("women-s-fashion");
        assertThat(categoryCaptor.getValue().getActive()).isTrue();
        assertThat(categoryCaptor.getValue().getVisible()).isTrue();
    }

    @Test
    void create_shouldRejectDuplicateName() {
        CategoryRequest request = CategoryRequest.builder()
                .categoryName("Fashion")
                .description("Fashion")
                .build();
        when(categoryRepository.existsByCategoryNameIgnoreCase("Fashion")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(request)).isInstanceOf(AppException.class);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void update_shouldRejectSelfParent() {
        Category category = Category.builder()
                .id(1L)
                .categoryName("Fashion")
                .slug("fashion")
                .description("Fashion")
                .active(true)
                .visible(true)
                .build();
        CategoryRequest request = CategoryRequest.builder()
                .categoryName("Fashion")
                .description("Fashion")
                .parentId(1L)
                .build();

        when(categoryRepository.findWithParentCategoryById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> categoryService.update(1L, request)).isInstanceOf(AppException.class);
    }
}
