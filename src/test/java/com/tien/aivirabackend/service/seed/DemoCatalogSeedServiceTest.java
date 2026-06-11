package com.tien.aivirabackend.service.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tien.aivirabackend.constant.BookFormat;
import com.tien.aivirabackend.constant.MediaType;
import com.tien.aivirabackend.constant.ProductStatus;
import com.tien.aivirabackend.domain.entity.catalog.Category;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.repository.CategoryRepository;
import com.tien.aivirabackend.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class DemoCatalogSeedServiceTest {
    @Mock
    CategoryRepository categoryRepository;

    @Mock
    ProductRepository productRepository;

    Map<String, Category> categoriesBySlug;
    Map<String, Product> productsBySku;
    Map<String, Product> productsBySlug;
    AtomicLong categoryId;
    AtomicLong productId;

    DemoCatalogSeedServiceImpl seedService;

    @BeforeEach
    void setUp() {
        categoriesBySlug = new LinkedHashMap<>();
        productsBySku = new LinkedHashMap<>();
        productsBySlug = new LinkedHashMap<>();
        categoryId = new AtomicLong(1);
        productId = new AtomicLong(1);
        seedService = new DemoCatalogSeedServiceImpl(categoryRepository, productRepository);

        when(categoryRepository.findBySlug(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(categoriesBySlug.get(invocation.getArgument(0))));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(categoryId.getAndIncrement());
            categoriesBySlug.put(category.getSlug(), category);
            return category;
        });
        when(productRepository.findBySku(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(productsBySku.get(invocation.getArgument(0))));
        when(productRepository.findBySlug(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(productsBySlug.get(invocation.getArgument(0))));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(productId.getAndIncrement());
            productsBySku.put(product.getSku(), product);
            productsBySlug.put(product.getSlug(), product);
            return product;
        });
    }

    @Test
    void seedDemoCatalog_shouldCreateBookstoreCategoriesAndBooks() {
        seedService.seedDemoCatalog();

        assertThat(categoriesBySlug).hasSize(13);
        assertThat(categoriesBySlug)
                .containsKeys(
                        "fiction",
                        "non-fiction",
                        "business",
                        "technology",
                        "children",
                        "vietnamese-books",
                        "fantasy",
                        "mystery",
                        "biography",
                        "self-help",
                        "programming",
                        "ai-data",
                        "picture-books");
        assertThat(categoriesBySlug.get("fantasy").getParentCategory()).isEqualTo(categoriesBySlug.get("fiction"));

        assertThat(productsBySku).hasSize(30);
        Product firstBook = productsBySku.get("BOOK-001");
        assertThat(firstBook.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(firstBook.getActive()).isTrue();
        assertThat(firstBook.getBookAuthor()).isNotBlank();
        assertThat(firstBook.getIsbn()).hasSizeLessThanOrEqualTo(20);
        assertThat(firstBook.getPublisher()).isNotBlank();
        assertThat(firstBook.getPublicationYear()).isBetween(1000, 2027);
        assertThat(firstBook.getBookLanguage()).isNotBlank();
        assertThat(firstBook.getPageCount()).isPositive();
        assertThat(firstBook.getBookFormat()).isIn(BookFormat.PAPERBACK, BookFormat.HARDCOVER);
        assertThat(firstBook.getStockQuantity()).isPositive();
        assertThat(firstBook.getSoldCount()).isPositive();
        assertThat(firstBook.getThumbnailUrl()).startsWith("https://picsum.photos/seed/aivira-book-");
        assertThat(firstBook.getProductVariations()).hasSize(1);
        assertThat(firstBook.getProductMedia()).hasSize(1);
        assertThat(firstBook.getProductMedia().iterator().next().getMediaType()).isEqualTo(MediaType.IMAGE);
        assertThat(firstBook.getProductMedia().iterator().next().getPrimary()).isTrue();
        assertThat(productsBySku.values().stream().filter(Product::getFeatured).count())
                .isEqualTo(8);
        assertThat(productsBySku.values().stream()
                        .filter(product -> product.getStockQuantity() <= 5)
                        .count())
                .isGreaterThanOrEqualTo(4);
    }

    @Test
    void seedDemoCatalog_whenRerun_shouldNotDuplicateData() {
        seedService.seedDemoCatalog();
        seedService.seedDemoCatalog();

        assertThat(categoriesBySlug).hasSize(13);
        assertThat(productsBySku).hasSize(30);
    }

    @Test
    void seedDemoCatalog_whenProductExists_shouldPreserveExistingProduct() {
        Product existing = Product.builder()
                .sku("BOOK-001")
                .slug("the-last-library")
                .productName("Admin Edited Title")
                .description("Admin data")
                .bookAuthor("Admin")
                .status(ProductStatus.ACTIVE)
                .active(true)
                .build();
        productsBySku.put(existing.getSku(), existing);
        productsBySlug.put(existing.getSlug(), existing);

        seedService.seedDemoCatalog();

        assertThat(productsBySku).hasSize(30);
        assertThat(productsBySku.get("BOOK-001").getProductName()).isEqualTo("Admin Edited Title");
    }
}
