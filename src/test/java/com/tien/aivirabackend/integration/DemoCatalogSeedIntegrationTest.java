package com.tien.aivirabackend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.tien.aivirabackend.repository.CategoryRepository;
import com.tien.aivirabackend.repository.ProductMediaRepository;
import com.tien.aivirabackend.repository.ProductRepository;
import com.tien.aivirabackend.repository.ProductVariationRepository;
import com.tien.aivirabackend.service.seed.DemoCatalogSeedService;

class DemoCatalogSeedIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    DemoCatalogSeedService demoCatalogSeedService;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    ProductVariationRepository productVariationRepository;

    @Autowired
    ProductMediaRepository productMediaRepository;

    @Test
    void demoCatalogSeed_shouldCreateIdempotentPublicCatalogData() throws Exception {
        demoCatalogSeedService.seedDemoCatalog();

        assertThat(categoryRepository.count()).isEqualTo(13);
        assertThat(productRepository.count()).isEqualTo(30);
        assertThat(productVariationRepository.count()).isEqualTo(30);
        assertThat(productMediaRepository.count()).isEqualTo(30);

        mockMvc.perform(get("/products").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(30))
                .andExpect(jsonPath("$.data.data[0].bookAuthor").isNotEmpty());

        mockMvc.perform(get("/storefront/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.featuredBooks").isArray())
                .andExpect(jsonPath("$.data.featuredBooks.length()").value(8))
                .andExpect(jsonPath("$.data.newArrivals").isArray())
                .andExpect(jsonPath("$.data.bestsellingBooks").isArray())
                .andExpect(jsonPath("$.data.categoryHighlights").isArray());

        demoCatalogSeedService.seedDemoCatalog();

        assertThat(categoryRepository.count()).isEqualTo(13);
        assertThat(productRepository.count()).isEqualTo(30);
        assertThat(productVariationRepository.count()).isEqualTo(30);
        assertThat(productMediaRepository.count()).isEqualTo(30);
    }
}
