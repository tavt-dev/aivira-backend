package com.tien.aivirabackend.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tien.aivirabackend.config.properties.AiAdviceProperties;
import com.tien.aivirabackend.constant.ProductStatus;
import com.tien.aivirabackend.domain.entity.catalog.Category;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class AiCatalogRankerTest {
    @Mock
    private ProductRepository productRepository;

    @Test
    void excludesUnrelatedBooksAndMatchesVietnameseWithoutAccents() {
        AiCatalogRanker ranker = ranker(30);
        Product matching = product(1L, "Tư duy lập trình", "Nguyễn Văn A", "Công nghệ", "Tiếng Việt", "150000");
        Product unrelated = product(2L, "Nấu ăn gia đình", "Trần B", "Ẩm thực", "Tiếng Việt", "120000");
        when(productRepository.findByActiveTrueAndStatusAndStockQuantityGreaterThan(ProductStatus.ACTIVE, 0))
                .thenReturn(List.of(unrelated, matching));

        List<Product> result = ranker
                .rank(profile(List.of("tu duy lap trinh"), List.of(), List.of(), List.of(), null, null), Set.of());

        assertThat(result).containsExactly(matching);
    }

    @Test
    void appliesPriceAndLanguageHardFiltersAndResultLimit() {
        AiCatalogRanker ranker = ranker(1);
        Product eligible = product(1L, "Clean Code", "Robert Martin", "Programming", "English", "180000");
        Product tooExpensive = product(2L, "Refactoring", "Martin Fowler", "Programming", "English", "350000");
        Product wrongLanguage = product(3L, "Code Complete", "Steve McConnell", "Programming", "Vietnamese", "170000");
        when(productRepository.findByActiveTrueAndStatusAndStockQuantityGreaterThan(ProductStatus.ACTIVE, 0))
                .thenReturn(List.of(eligible, tooExpensive, wrongLanguage));

        List<Product> result = ranker.rank(profile(List.of(), List.of(), List.of(), List.of("English"),
                new BigDecimal("100000"), new BigDecimal("200000")), Set.of());

        assertThat(result).containsExactly(eligible);
    }

    @Test
    void understandsProgrammingAndChildrenSynonyms() {
        AiCatalogRanker ranker = ranker(30);
        Product java = product(1L, "Java APIs in Practice", "Tien Pham", "Programming", "Vietnamese", "195000");
        Product children = product(2L, "Milo Finds A Map", "Lucy Hart", "Picture Books", "Vietnamese", "150000");
        when(productRepository.findByActiveTrueAndStatusAndStockQuantityGreaterThan(ProductStatus.ACTIVE, 0))
                .thenReturn(List.of(java, children));

        assertThat(ranker.rank(profile(List.of("code"), List.of(), List.of(), List.of(), null, null), Set.of()))
                .contains(java).doesNotContain(children);
        assertThat(ranker.rank(profile(List.of("trẻ em"), List.of(), List.of(), List.of(), null, null), Set.of()))
                .contains(children).doesNotContain(java);
    }

    private AiCatalogRanker ranker(int resultLimit) {
        return new AiCatalogRanker(productRepository,
                new AiAdviceProperties("gemini", 30, 10, 200, resultLimit, 30, false));
    }

    private AiSearchProfile profile(List<String> terms, List<String> categories, List<String> authors,
            List<String> languages, BigDecimal minPrice, BigDecimal maxPrice) {
        return new AiSearchProfile(false, "", "", terms, categories, authors, languages, minPrice, maxPrice, List.of());
    }

    private Product product(Long id, String name, String author, String category, String language, String price) {
        return Product.builder().id(id).sku("SKU-" + id).productName(name).slug("book-" + id).description(name)
                .bookAuthor(author).bookLanguage(language).price(new BigDecimal(price)).stockQuantity(10).soldCount(0)
                .active(true).status(ProductStatus.ACTIVE).category(Category.builder().id(id).categoryName(category)
                        .slug("category-" + id).description(category).build())
                .build();
    }
}
