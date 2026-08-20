package com.tien.aivirabackend.domain.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.tien.aivirabackend.domain.entity.catalog.Product;

class ProductMapperTest {
    private final ProductMapper productMapper = new ProductMapper();

    @Test
    void toResponse_shouldExposeComputedAverageRatingAndSoldCount() {
        Product product = Product.builder().id(10L).productName("Decision Logs").averageRating(new BigDecimal("4.50"))
                .soldCount(7).build();

        var response = productMapper.toResponse(product);

        assertThat(response.getAverageRating()).isEqualByComparingTo("4.50");
        assertThat(response.getSoldCount()).isEqualTo(7);
    }
}
