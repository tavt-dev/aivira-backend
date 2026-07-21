package com.tien.aivirabackend.service.storefront;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.tien.aivirabackend.constant.ProductStatus;
import com.tien.aivirabackend.domain.entity.catalog.Category;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.mapper.ProductMapper;
import com.tien.aivirabackend.repository.CategoryRepository;
import com.tien.aivirabackend.repository.ProductRepository;
import com.tien.aivirabackend.repository.projection.CategoryHighlightProjection;
import com.tien.aivirabackend.service.catalog.ProductSpecifications;
import com.tien.aivirabackend.service.blog.BlogPostService;

@ExtendWith(MockitoExtension.class)
class StorefrontServiceImplTest {
    @Mock
    ProductRepository productRepository;

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    BlogPostService blogPostService;

    StorefrontServiceImpl storefrontService;

    @BeforeEach
    void setUp() {
        storefrontService = new StorefrontServiceImpl(
                productRepository, categoryRepository, new ProductSpecifications(), new ProductMapper(), blogPostService);
    }

    @Test
    void getHome_shouldReturnHomepageSections() {
        Product product = product("BOOK-1", "Book 1", 10);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)))
                .thenReturn(new PageImpl<>(List.of(product("BOOK-2", "Book 2", 3))))
                .thenReturn(new PageImpl<>(List.of(product("BOOK-3", "Book 3", 20))));
        when(categoryRepository.findCategoryHighlights(any(Pageable.class)))
                .thenReturn(List.of(categoryHighlight(1L, "Fiction", 12L)));
        when(blogPostService.getLatestPosts(4)).thenReturn(List.of());

        var response = storefrontService.getHome();

        assertThat(response.getFeaturedBooks()).hasSize(1);
        assertThat(response.getNewArrivals()).hasSize(1);
        assertThat(response.getBestsellingBooks()).hasSize(1);
        assertThat(response.getCategoryHighlights()).hasSize(1);
        assertThat(response.getCategoryHighlights().getFirst().getBookCount()).isEqualTo(12L);
        assertThat(response.getLatestPosts()).isEmpty();
        verify(productRepository, times(3)).findAll(any(Specification.class), any(Pageable.class));
        verify(categoryRepository).findCategoryHighlights(argThat(pageable -> pageable.getPageSize() == 6));
    }

    @Test
    void getHome_whenNoData_shouldReturnEmptyLists() {
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(categoryRepository.findCategoryHighlights(any(Pageable.class))).thenReturn(List.of());
        when(blogPostService.getLatestPosts(4)).thenReturn(List.of());

        var response = storefrontService.getHome();

        assertThat(response.getFeaturedBooks()).isEmpty();
        assertThat(response.getNewArrivals()).isEmpty();
        assertThat(response.getBestsellingBooks()).isEmpty();
        assertThat(response.getCategoryHighlights()).isEmpty();
        assertThat(response.getLatestPosts()).isEmpty();
    }

    private Product product(String sku, String name, int soldCount) {
        return Product.builder()
                .id((long) soldCount)
                .category(Category.builder()
                        .id(1L)
                        .categoryName("Books")
                        .slug("books")
                        .description("Books")
                        .active(true)
                        .visible(true)
                        .build())
                .sku(sku)
                .productName(name)
                .slug(name.toLowerCase().replace(" ", "-"))
                .description("Description")
                .bookAuthor("Author")
                .price(BigDecimal.TEN)
                .stockQuantity(5)
                .soldCount(soldCount)
                .active(true)
                .featured(true)
                .status(ProductStatus.ACTIVE)
                .build();
    }

    private CategoryHighlightProjection categoryHighlight(Long id, String name, Long count) {
        return new CategoryHighlightProjection() {
            @Override
            public Long getCategoryId() {
                return id;
            }

            @Override
            public String getCategoryName() {
                return name;
            }

            @Override
            public String getSlug() {
                return name.toLowerCase();
            }

            @Override
            public String getDescription() {
                return name;
            }

            @Override
            public String getImageUrl() {
                return null;
            }

            @Override
            public String getImagePublicId() {
                return null;
            }

            @Override
            public Integer getDisplayOrder() {
                return 0;
            }

            @Override
            public Long getBookCount() {
                return count;
            }
        };
    }
}
