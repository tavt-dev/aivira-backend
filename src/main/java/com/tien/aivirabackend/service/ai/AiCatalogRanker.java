package com.tien.aivirabackend.service.ai;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.tien.aivirabackend.config.properties.AiAdviceProperties;
import com.tien.aivirabackend.constant.ProductStatus;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AiCatalogRanker {
    private static final Set<String> STOP_WORDS = Set.of("toi", "muon", "tim", "sach", "cho", "cua", "mot", "nhung",
            "va", "de", "doc", "the", "book", "find", "want", "with", "for", "and", "read");
    private static final List<Set<String>> SYNONYM_GROUPS = List.of(
            Set.of("code", "coding", "programming", "lap trinh", "tin hoc", "cong nghe thong tin", "java", "spring",
                    "api", "software", "phan mem", "refactoring"),
            Set.of("tre em", "thieu nhi", "children", "child", "kids", "kid", "picture books", "picture book",
                    "truyen tranh", "little", "goodnight"),
            Set.of("ai", "artificial intelligence", "tri tue nhan tao", "machine learning", "data", "du lieu"));

    private final ProductRepository productRepository;
    private final AiAdviceProperties properties;

    public List<Product> rank(AiSearchProfile profile, Set<Long> purchased) {
        return productRepository.findByActiveTrueAndStatusAndStockQuantityGreaterThan(ProductStatus.ACTIVE, 0).stream()
                .filter(product -> profile.minPrice() == null || product.getPrice().compareTo(profile.minPrice()) >= 0)
                .filter(product -> profile.maxPrice() == null || product.getPrice().compareTo(profile.maxPrice()) <= 0)
                .filter(product -> profile.languages() == null || profile.languages().isEmpty()
                        || containsAny(product.getBookLanguage(), profile.languages()))
                .filter(product -> !hasTextHints(profile) || lexicalScore(product, profile) > 0)
                .sorted(Comparator.comparingDouble((Product product) -> score(product, profile, purchased)).reversed()
                        .thenComparing(Product::getId))
                .limit(properties.candidateLimit()).limit(properties.resultLimit()).toList();
    }

    public AiSearchProfile fallbackProfile(String content) {
        List<String> terms = Arrays.stream(normalize(content).split("\\s+")).filter(term -> term.length() >= 3)
                .filter(term -> !STOP_WORDS.contains(term)).distinct().limit(8).toList();
        return new AiSearchProfile(false, "", content, terms, List.of(), List.of(), List.of(), null, null, List.of());
    }

    public List<String> matchedCriteria(Product product, AiSearchProfile profile) {
        String haystack = haystack(product);
        return Stream.of(profile.searchTerms(), profile.categoryHints(), profile.authorHints()).filter(Objects::nonNull)
                .flatMap(Collection::stream).filter(StringUtils::hasText).filter(value -> matchesValue(haystack, value))
                .distinct().limit(4).toList();
    }

    private boolean hasTextHints(AiSearchProfile profile) {
        return hasValues(profile.searchTerms()) || hasValues(profile.categoryHints())
                || hasValues(profile.authorHints());
    }

    private boolean hasValues(List<String> values) {
        return values != null && values.stream().anyMatch(StringUtils::hasText);
    }

    private int lexicalScore(Product product, AiSearchProfile profile) {
        String haystack = haystack(product);
        return matchCount(haystack, profile.searchTerms()) + matchCount(haystack, profile.categoryHints())
                + matchCount(haystack, profile.authorHints());
    }

    private double score(Product product, AiSearchProfile profile, Set<Long> purchased) {
        String haystack = haystack(product);
        double score = matchCount(haystack, profile.searchTerms()) * 12;
        score += matchCount(haystack, profile.categoryHints()) * 10;
        score += matchCount(haystack, profile.authorHints()) * 12;
        score += product.getAverageRating() == null ? 0 : product.getAverageRating().doubleValue() * 2;
        score += Math.log1p(Math.max(0, product.getSoldCount())) * 2;
        if (purchased.contains(product.getId()))
            score -= 25;
        return score;
    }

    private String haystack(Product product) {
        return String.join(" ", safe(product.getProductName()), safe(product.getBookAuthor()),
                safe(product.getDescription()),
                product.getCategory() == null ? "" : safe(product.getCategory().getCategoryName()));
    }

    private int matchCount(String haystack, List<String> values) {
        if (values == null)
            return 0;
        String normalizedHaystack = normalize(haystack);
        return (int) values.stream().filter(StringUtils::hasText)
                .filter(value -> expandedValues(value).stream().anyMatch(normalizedHaystack::contains)).count();
    }

    private boolean matchesValue(String haystack, String value) {
        String normalizedHaystack = normalize(haystack);
        return expandedValues(value).stream().anyMatch(normalizedHaystack::contains);
    }

    private Set<String> expandedValues(String value) {
        String normalized = normalize(value);
        return SYNONYM_GROUPS.stream().filter(
                group -> group.stream().anyMatch(term -> normalized.contains(term) || term.contains(normalized)))
                .findFirst().orElse(Set.of(normalized));
    }

    private boolean containsAny(String value, List<String> expected) {
        if (!StringUtils.hasText(value))
            return false;
        String normalized = normalize(value);
        return expected.stream().filter(StringUtils::hasText).anyMatch(item -> normalized.contains(normalize(item)));
    }

    private String normalize(String value) {
        String decomposed = Normalizer.normalize(safe(value), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "").replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
