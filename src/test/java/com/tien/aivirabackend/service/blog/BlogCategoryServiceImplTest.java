package com.tien.aivirabackend.service.blog;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tien.aivirabackend.domain.entity.blog.BlogCategory;
import com.tien.aivirabackend.domain.mapper.BlogMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.BlogErrorCode;
import com.tien.aivirabackend.repository.BlogCategoryRepository;
import com.tien.aivirabackend.repository.BlogPostRepository;

@ExtendWith(MockitoExtension.class)
class BlogCategoryServiceImplTest {
    @Mock
    BlogCategoryRepository categoryRepository;

    @Mock
    BlogPostRepository postRepository;

    BlogCategoryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BlogCategoryServiceImpl(categoryRepository, postRepository, new BlogMapper());
    }

    @Test
    void deleteCategory_whenCategoryIsUsed_shouldRejectDeletion() {
        BlogCategory category =
                BlogCategory.builder().id(1L).name("News").slug("news").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(postRepository.existsByCategory_IdAndDeletedAtIsNull(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteCategory(1L))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(BlogErrorCode.BLOG_CATEGORY_IN_USE);
        verify(categoryRepository, never()).delete(any());
    }
}
