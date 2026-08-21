package com.tien.aivirabackend.service.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.tien.aivirabackend.constant.*;
import com.tien.aivirabackend.domain.entity.catalog.*;

class RagBookDocumentTest {
    @Test
    void from_shouldBuildStableSearchDocumentAndPayload() {
        Category category = Category.builder().id(7L).categoryName("Lập trình").build();
        Product product = Product.builder().id(11L).productName("Clean Code").description("<p>Mã nguồn sạch</p>")
                .bookAuthor("Robert Martin").publisher("Prentice Hall").isbn("9780132350884")
                .bookLanguage("English").bookFormat(BookFormat.PAPERBACK).publicationYear(2008)
                .price(BigDecimal.valueOf(250000)).active(true).status(ProductStatus.ACTIVE).category(category).build();

        RagBookDocument first = RagBookDocument.from(product, "gemini", "gemini-embedding-001");
        RagBookDocument second = RagBookDocument.from(product, "gemini", "gemini-embedding-001");

        assertThat(first.content()).contains("Clean Code", "Robert Martin", "Mã nguồn sạch", "Lập trình");
        assertThat(first.content()).doesNotContain("<p>");
        assertThat(first.contentHash()).isEqualTo(second.contentHash()).hasSize(64);
        assertThat(first.payload()).containsEntry("productId", 11L).containsEntry("contentHash", first.contentHash());
    }

    @Test
    void contentHash_shouldChangeWhenSemanticContentChanges() {
        Category category = Category.builder().id(1L).categoryName("Kinh tế").build();
        Product product = Product.builder().id(1L).productName("Book").description("First description")
                .bookAuthor("Author").price(BigDecimal.TEN).active(true).status(ProductStatus.ACTIVE)
                .category(category).build();
        String before = RagBookDocument.from(product, "openai", "text-embedding-3-small").contentHash();
        product.setDescription("A different description");
        assertThat(RagBookDocument.from(product, "openai", "text-embedding-3-small").contentHash()).isNotEqualTo(before);
    }
}
