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

    private void saveProduct(String sku, String slug, ProductStatus status, Category category) {
        Product product = Product.builder()
                .category(category)
                .sku(sku)
                .productName(slug)
                .slug(slug)
                .description("Product description")
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
