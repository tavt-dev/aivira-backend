package com.tien.aivirabackend.service.seed;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tien.aivirabackend.constant.BookFormat;
import com.tien.aivirabackend.constant.MediaType;
import com.tien.aivirabackend.constant.ProductStatus;
import com.tien.aivirabackend.domain.entity.catalog.Category;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.entity.catalog.ProductMedia;
import com.tien.aivirabackend.domain.entity.catalog.ProductVariation;
import com.tien.aivirabackend.repository.CategoryRepository;
import com.tien.aivirabackend.repository.ProductRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DemoCatalogSeedServiceImpl implements DemoCatalogSeedService {
    static final String COVER_BASE_URL = "https://picsum.photos/seed/aivira-book-";

    CategoryRepository categoryRepository;
    ProductRepository productRepository;

    @Override
    @Transactional
    public void seedDemoCatalog() {
        log.info("[INIT] Seeding demo bookstore catalog...");

        Map<String, Category> categories = seedCategories();
        int createdBooks = 0;
        for (BookSeed book : demoBooks()) {
            if (productRepository.findBySku(book.sku()).isPresent()
                    || productRepository.findBySlug(book.slug()).isPresent()) {
                continue;
            }
            Category category = categories.get(book.categorySlug());
            if (category == null) {
                log.warn("[INIT] Skip demo book '{}': category '{}' not found", book.sku(), book.categorySlug());
                continue;
            }
            productRepository.save(toProduct(book, category));
            createdBooks++;
        }

        log.info("[INIT] Demo bookstore catalog seeded. Created {} new books.", createdBooks);
    }

    private Map<String, Category> seedCategories() {
        Category fiction = getOrCreateCategory(root("Fiction", "fiction", "Literary and genre fiction.", 10));
        Category nonFiction =
                getOrCreateCategory(root("Non-Fiction", "non-fiction", "Knowledge, history, and real stories.", 20));
        Category business = getOrCreateCategory(root("Business", "business", "Business, finance, and leadership.", 30));
        Category technology =
                getOrCreateCategory(root("Technology", "technology", "Programming, AI, and digital craft.", 40));
        Category children = getOrCreateCategory(root("Children", "children", "Books for young readers.", 50));
        Category vietnamese =
                getOrCreateCategory(root("Vietnamese Books", "vietnamese-books", "Vietnamese-language books.", 60));

        return Map.ofEntries(
                Map.entry("fiction", fiction),
                Map.entry("non-fiction", nonFiction),
                Map.entry("business", business),
                Map.entry("technology", technology),
                Map.entry("children", children),
                Map.entry("vietnamese-books", vietnamese),
                Map.entry(
                        "fantasy",
                        getOrCreateCategory(child("Fantasy", "fantasy", "Fantasy adventures.", 11, fiction))),
                Map.entry(
                        "mystery",
                        getOrCreateCategory(child("Mystery", "mystery", "Mystery and crime novels.", 12, fiction))),
                Map.entry(
                        "biography",
                        getOrCreateCategory(child("Biography", "biography", "Lives and memoirs.", 21, nonFiction))),
                Map.entry(
                        "self-help",
                        getOrCreateCategory(child("Self-Help", "self-help", "Personal growth.", 22, nonFiction))),
                Map.entry(
                        "programming",
                        getOrCreateCategory(
                                child("Programming", "programming", "Software engineering.", 41, technology))),
                Map.entry(
                        "ai-data",
                        getOrCreateCategory(child("AI & Data", "ai-data", "AI, ML, and data.", 42, technology))),
                Map.entry(
                        "picture-books",
                        getOrCreateCategory(
                                child("Picture Books", "picture-books", "Illustrated books.", 51, children))));
    }

    private Category getOrCreateCategory(Category seed) {
        return categoryRepository.findBySlug(seed.getSlug()).orElseGet(() -> categoryRepository.save(seed));
    }

    private Category root(String name, String slug, String description, int displayOrder) {
        return Category.builder()
                .categoryName(name)
                .slug(slug)
                .description(description)
                .displayOrder(displayOrder)
                .active(true)
                .visible(true)
                .build();
    }

    private Category child(String name, String slug, String description, int displayOrder, Category parent) {
        return Category.builder()
                .categoryName(name)
                .slug(slug)
                .description(description)
                .displayOrder(displayOrder)
                .parentCategory(parent)
                .active(true)
                .visible(true)
                .build();
    }

    private Product toProduct(BookSeed seed, Category category) {
        Product product = Product.builder()
                .category(category)
                .sku(seed.sku())
                .productName(seed.title())
                .slug(seed.slug())
                .description(seed.description())
                .brand(seed.brand())
                .bookAuthor(seed.author())
                .isbn(seed.isbn())
                .publisher(seed.publisher())
                .publicationYear(seed.publicationYear())
                .bookLanguage(seed.language())
                .pageCount(seed.pageCount())
                .bookFormat(seed.format())
                .dimensions(seed.dimensions())
                .thumbnailUrl(seed.coverUrl())
                .thumbnailPublicId(seed.coverPublicId())
                .price(seed.price())
                .originalPrice(seed.originalPrice())
                .discountPercentage(seed.discountPercentage())
                .stockQuantity(seed.stockQuantity())
                .soldCount(seed.soldCount())
                .active(true)
                .featured(seed.featured())
                .status(ProductStatus.ACTIVE)
                .build();
        product.getProductVariations()
                .add(ProductVariation.builder()
                        .product(product)
                        .sku(seed.sku() + "-" + variationSuffix(seed.format()))
                        .color("Default")
                        .size(formatLabel(seed.format()))
                        .additionalPrice(BigDecimal.ZERO)
                        .stockQuantity(seed.stockQuantity())
                        .imageUrl(seed.coverUrl())
                        .imagePublicId(seed.coverPublicId() + "-variation")
                        .active(true)
                        .build());
        product.getProductMedia()
                .add(ProductMedia.builder()
                        .product(product)
                        .mediaUrl(seed.coverUrl())
                        .mediaPublicId(seed.coverPublicId())
                        .mediaType(MediaType.IMAGE)
                        .altText(seed.title() + " cover")
                        .sortOrder(0)
                        .primary(true)
                        .active(true)
                        .build());
        return product;
    }

    private String variationSuffix(BookFormat format) {
        return switch (format) {
            case HARDCOVER -> "HC";
            case EBOOK -> "EB";
            case BOXSET -> "BX";
            case OTHER -> "OT";
            default -> "PB";
        };
    }

    private String formatLabel(BookFormat format) {
        return switch (format) {
            case HARDCOVER -> "Hardcover";
            case EBOOK -> "Ebook";
            case BOXSET -> "Boxset";
            case OTHER -> "Other";
            default -> "Paperback";
        };
    }

    private List<BookSeed> demoBooks() {
        return List.of(
                book(
                        1,
                        "Fiction",
                        "The Last Library",
                        "Maya Rivers",
                        "Silverleaf Press",
                        2022,
                        328,
                        "fantasy",
                        true,
                        24,
                        81),
                book(
                        2,
                        "Fiction",
                        "Midnight in Old Saigon",
                        "Lan Nguyen",
                        "Aivira Classics",
                        2021,
                        296,
                        "mystery",
                        true,
                        7,
                        64),
                book(
                        3,
                        "Fiction",
                        "The Paper Kingdom",
                        "Elias Stone",
                        "North Star Books",
                        2019,
                        412,
                        "fantasy",
                        false,
                        18,
                        52),
                book(
                        4,
                        "Fiction",
                        "Rain Over Hanoi",
                        "Minh Tran",
                        "Lotus House",
                        2020,
                        244,
                        "vietnamese-books",
                        false,
                        5,
                        47),
                book(
                        5,
                        "Fiction",
                        "A Study in Blue Ink",
                        "Clara Wells",
                        "Beacon Press",
                        2018,
                        352,
                        "mystery",
                        false,
                        16,
                        39),
                book(
                        6,
                        "Fiction",
                        "The Star Cartographer",
                        "Jon Bell",
                        "Orbit Road",
                        2023,
                        480,
                        "fantasy",
                        true,
                        11,
                        92),
                book(
                        7,
                        "Non-Fiction",
                        "Quiet Focus",
                        "Nora Pham",
                        "Mindful Work",
                        2022,
                        224,
                        "self-help",
                        false,
                        9,
                        35),
                book(
                        8,
                        "Non-Fiction",
                        "Habits That Compound",
                        "Daniel Ho",
                        "Forward Press",
                        2021,
                        256,
                        "self-help",
                        false,
                        3,
                        76),
                book(
                        9,
                        "Non-Fiction",
                        "Letters From A Founder",
                        "Grace Morgan",
                        "North Star Books",
                        2020,
                        288,
                        "biography",
                        false,
                        13,
                        28),
                book(
                        10,
                        "Non-Fiction",
                        "The Long Interview",
                        "David Le",
                        "Aivira Classics",
                        2017,
                        368,
                        "biography",
                        false,
                        21,
                        18),
                book(
                        11,
                        "Business",
                        "Practical Pricing",
                        "Hannah Reed",
                        "Market Craft",
                        2023,
                        240,
                        "business",
                        true,
                        8,
                        70),
                book(
                        12,
                        "Business",
                        "Small Teams, Clear Systems",
                        "Victor Nguyen",
                        "Market Craft",
                        2022,
                        304,
                        "business",
                        false,
                        20,
                        43),
                book(
                        13,
                        "Business",
                        "Cashflow Notes",
                        "Peter Lam",
                        "Ledger House",
                        2021,
                        216,
                        "business",
                        false,
                        4,
                        31),
                book(
                        14,
                        "Business",
                        "The Operator's Handbook",
                        "Ivy Chen",
                        "Forward Press",
                        2024,
                        336,
                        "business",
                        true,
                        12,
                        88),
                book(
                        15,
                        "Technology",
                        "Java APIs in Practice",
                        "Tien Pham",
                        "Code Harbor",
                        2024,
                        520,
                        "programming",
                        true,
                        15,
                        95),
                book(
                        16,
                        "Technology",
                        "Spring Security Field Guide",
                        "An Le",
                        "Code Harbor",
                        2023,
                        448,
                        "programming",
                        false,
                        6,
                        67),
                book(
                        17,
                        "Technology",
                        "Data Pipelines Made Simple",
                        "Mina Park",
                        "Data Shelf",
                        2022,
                        390,
                        "ai-data",
                        false,
                        17,
                        54),
                book(
                        18,
                        "Technology",
                        "Machine Learning Notes",
                        "Oscar Kim",
                        "Data Shelf",
                        2021,
                        430,
                        "ai-data",
                        false,
                        2,
                        83),
                book(
                        19,
                        "Technology",
                        "Refactoring Legacy Services",
                        "Khoa Dang",
                        "Code Harbor",
                        2020,
                        360,
                        "programming",
                        false,
                        10,
                        41),
                book(
                        20,
                        "Children",
                        "The Little Cloud",
                        "Anna Vu",
                        "Bright Kids",
                        2023,
                        40,
                        "picture-books",
                        true,
                        23,
                        58),
                book(
                        21,
                        "Children",
                        "Milo Finds A Map",
                        "Lucy Hart",
                        "Bright Kids",
                        2022,
                        48,
                        "picture-books",
                        false,
                        19,
                        45),
                book(
                        22,
                        "Children",
                        "Goodnight Bamboo",
                        "Mai Hoang",
                        "Lotus House",
                        2021,
                        36,
                        "picture-books",
                        false,
                        5,
                        37),
                book(
                        23,
                        "Vietnamese Books",
                        "Nhung Ngay Rong Choi",
                        "Bao Chau",
                        "Lotus House",
                        2020,
                        220,
                        "vietnamese-books",
                        false,
                        14,
                        29),
                book(
                        24,
                        "Vietnamese Books",
                        "Quan Ca Phe Cu",
                        "Thanh Bui",
                        "Aivira Classics",
                        2019,
                        260,
                        "vietnamese-books",
                        false,
                        8,
                        21),
                book(
                        25,
                        "Vietnamese Books",
                        "Duong Ve Mua Ha",
                        "Linh Dan",
                        "Lotus House",
                        2023,
                        198,
                        "vietnamese-books",
                        false,
                        1,
                        62),
                book(
                        26,
                        "Fiction",
                        "The Clockmaker's Guest",
                        "Henry Fox",
                        "Beacon Press",
                        2016,
                        384,
                        "mystery",
                        false,
                        12,
                        26),
                book(
                        27,
                        "Non-Fiction",
                        "A Short History of Work",
                        "Evan Cole",
                        "Forward Press",
                        2018,
                        312,
                        "non-fiction",
                        false,
                        18,
                        33),
                book(
                        28,
                        "Technology",
                        "Clean Backend Patterns",
                        "Ravi Shah",
                        "Code Harbor",
                        2024,
                        410,
                        "programming",
                        true,
                        6,
                        79),
                book(
                        29,
                        "Business",
                        "Decision Logs",
                        "Maria Gomez",
                        "Market Craft",
                        2022,
                        208,
                        "business",
                        false,
                        9,
                        24),
                book(
                        30,
                        "Children",
                        "A Kite For Sunday",
                        "Sophie Lane",
                        "Bright Kids",
                        2020,
                        44,
                        "picture-books",
                        false,
                        25,
                        19));
    }

    private BookSeed book(
            int index,
            String brand,
            String title,
            String author,
            String publisher,
            int year,
            int pages,
            String categorySlug,
            boolean featured,
            int stock,
            int sold) {
        String padded = String.format("%03d", index);
        BigDecimal price = BigDecimal.valueOf(90000L + index * 7000L);
        return new BookSeed(
                "BOOK-" + padded,
                title,
                slug(title),
                "Demo bookstore title for " + title + ".",
                brand,
                author,
                "978604" + String.format("%07d", index),
                publisher,
                year,
                index <= 25 ? "Vietnamese" : "English",
                pages,
                index % 9 == 0 ? BookFormat.HARDCOVER : BookFormat.PAPERBACK,
                "20 x 13 x 2 cm",
                categorySlug,
                price,
                price.add(BigDecimal.valueOf(25000)),
                BigDecimal.valueOf(10),
                stock,
                sold,
                featured,
                COVER_BASE_URL + padded + "/600/800",
                "demo-catalog/book-" + padded);
    }

    private String slug(String title) {
        return title.toLowerCase()
                .replace("'", "")
                .replace("&", "and")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private record BookSeed(
            String sku,
            String title,
            String slug,
            String description,
            String brand,
            String author,
            String isbn,
            String publisher,
            Integer publicationYear,
            String language,
            Integer pageCount,
            BookFormat format,
            String dimensions,
            String categorySlug,
            BigDecimal price,
            BigDecimal originalPrice,
            BigDecimal discountPercentage,
            Integer stockQuantity,
            Integer soldCount,
            boolean featured,
            String coverUrl,
            String coverPublicId) {}
}
