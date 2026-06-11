package com.tien.aivirabackend.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.tien.aivirabackend.constant.ProductStatus;
import com.tien.aivirabackend.domain.entity.catalog.Category;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.entity.catalog.ProductVariation;
import com.tien.aivirabackend.repository.CategoryRepository;
import com.tien.aivirabackend.repository.ProductRepository;

class CatalogPublicIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    ProductRepository productRepository;

    @Test
    void publicCatalogEndpoints_shouldExposeOnlyVisibleActiveProductsWithoutToken() throws Exception {
        Category category = categoryRepository.save(Category.builder()
                .categoryName("Fashion")
                .slug("fashion")
                .description("Fashion")
                .displayOrder(0)
                .active(true)
                .visible(true)
                .build());
        saveProduct("ACTIVE-1", "active-product", ProductStatus.ACTIVE, category);
        saveProduct("DRAFT-1", "draft-product", ProductStatus.DRAFT, category);

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].slug").value("fashion"));

        mockMvc.perform(get("/products").param("keyword", "product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.data[0].slug").value("active-product"));

        mockMvc.perform(get("/products/{slug}", "active-product"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(get("/products/{slug}", "draft-product")).andExpect(status().isNotFound());
    }

    @Test
    void productSchema_shouldIncludeBookCatalogFields() {
        Integer columnCount = jdbcTemplate.queryForObject(
                """
				SELECT COUNT(*)
				FROM information_schema.columns
				WHERE table_schema = DATABASE()
				AND table_name = 'products'
				AND column_name IN (
					'book_author',
					'isbn',
					'publisher',
					'publication_year',
					'book_language',
					'page_count',
					'book_format',
					'dimensions'
				)
				""",
                Integer.class);

        org.assertj.core.api.Assertions.assertThat(columnCount).isEqualTo(8);
    }

    @Test
    void publicProductSearch_shouldFilterByBookMetadataAndSortByName() throws Exception {
        Category category = categoryRepository.save(Category.builder()
                .categoryName("Books")
                .slug("books")
                .description("Books")
                .displayOrder(0)
                .active(true)
                .visible(true)
                .build());
        saveProduct(
                "BOOK-1",
                "beta-book",
                "Beta Book",
                ProductStatus.ACTIVE,
                category,
                "Nguyen Nhat Anh",
                "Tre Publishing",
                "978-604-001");
        saveProduct(
                "BOOK-2",
                "alpha-book",
                "Alpha Book",
                ProductStatus.ACTIVE,
                category,
                "Ursula Le Guin",
                "Aivira Press",
                "978-604-002");

        mockMvc.perform(get("/products").param("author", "nguyen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.data[0].slug").value("beta-book"));

        mockMvc.perform(get("/products").param("publisher", "aivira"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.data[0].slug").value("alpha-book"));

        mockMvc.perform(get("/products").param("isbn", "002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.data[0].slug").value("alpha-book"));

        mockMvc.perform(get("/products").param("keyword", "guin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.data[0].slug").value("alpha-book"));

        mockMvc.perform(get("/products").param("sort", "name_asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data[0].slug").value("alpha-book"))
                .andExpect(jsonPath("$.data.data[1].slug").value("beta-book"));
    }

    private void saveProduct(String sku, String slug, ProductStatus status, Category category) {
        saveProduct(sku, slug, slug, status, category, "Unknown", null, null);
    }

    private void saveProduct(
            String sku,
            String slug,
            String productName,
            ProductStatus status,
            Category category,
            String bookAuthor,
            String publisher,
            String isbn) {
        Product product = Product.builder()
                .category(category)
                .sku(sku)
                .productName(productName)
                .slug(slug)
                .description("Product description")
                .bookAuthor(bookAuthor)
                .publisher(publisher)
                .isbn(isbn)
                .price(BigDecimal.valueOf(100))
                .stockQuantity(5)
                .soldCount(0)
                .active(true)
                .featured(false)
                .status(status)
                .build();
        ProductVariation variation = ProductVariation.builder()
                .product(product)
                .sku(sku + "-VAR")
                .color("Black")
                .size("M")
                .additionalPrice(BigDecimal.ZERO)
                .stockQuantity(5)
                .active(true)
                .build();
        product.getProductVariations().add(variation);
        productRepository.save(product);
    }
}
