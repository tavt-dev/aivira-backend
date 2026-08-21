package com.tien.aivirabackend.service.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Year;
import java.util.Locale;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.tien.aivirabackend.config.properties.CloudinaryProperties;
import com.tien.aivirabackend.constant.BookFormat;
import com.tien.aivirabackend.constant.MediaType;
import com.tien.aivirabackend.constant.ProductStatus;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.ProductCreateRequest;
import com.tien.aivirabackend.domain.dto.request.ProductMediaUpdateRequest;
import com.tien.aivirabackend.domain.dto.request.ProductUpdateRequest;
import com.tien.aivirabackend.domain.dto.request.ProductVariationRequest;
import com.tien.aivirabackend.domain.dto.request.StockUpdateRequest;
import com.tien.aivirabackend.domain.dto.response.ProductResponse;
import com.tien.aivirabackend.domain.entity.catalog.Category;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.entity.catalog.ProductMedia;
import com.tien.aivirabackend.domain.entity.catalog.ProductVariation;
import com.tien.aivirabackend.domain.mapper.ProductMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.CategoryErrorCode;
import com.tien.aivirabackend.exception.errorCode.ProductErrorCode;
import com.tien.aivirabackend.repository.CategoryRepository;
import com.tien.aivirabackend.repository.ProductMediaRepository;
import com.tien.aivirabackend.repository.ProductRepository;
import com.tien.aivirabackend.repository.ProductVariationRepository;
import com.tien.aivirabackend.service.auth.CurrentUserService;
import com.tien.aivirabackend.service.media.CloudinaryStorageService;
import com.tien.aivirabackend.service.media.CloudinaryUploadResult;
import com.tien.aivirabackend.service.media.FileValidatorService;
import com.tien.aivirabackend.service.ai.ProductCatalogChangedEvent;
import com.tien.aivirabackend.util.PageRequestUtils;
import com.tien.aivirabackend.util.SlugUtils;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j(topic = "PRODUCT-SERVICE")
public class ProductServiceImpl implements ProductService {
    private static final int PRODUCT_MEDIA_WIDTH = 1200;
    private static final int PRODUCT_MEDIA_HEIGHT = 1200;

    ProductRepository productRepository;
    ProductVariationRepository variationRepository;
    ProductMediaRepository mediaRepository;
    CategoryRepository categoryRepository;
    ProductMapper productMapper;
    FileValidatorService fileValidatorService;
    CloudinaryStorageService cloudinaryStorageService;
    CloudinaryProperties cloudinaryProperties;
    CurrentUserService currentUserService;
    ProductSpecifications productSpecifications;
    ProductStatusPolicy productStatusPolicy;
    ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getPublicProducts(String keyword, String categorySlug, String brand,
            String author, String publisher, String isbn, BigDecimal minPrice, BigDecimal maxPrice, Boolean available,
            String sort, int page, int size) {
        var productPage = productRepository
                .findAll(
                        productSpecifications.publicProducts(keyword, categorySlug, brand, author, publisher, isbn,
                                minPrice, maxPrice, available),
                        PageRequestUtils.of(page, size, resolvePublicSort(sort)))
                .map(productMapper::toResponse);
        return PageResponse.from(productPage);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getPublicProduct(String slug) {
        Product product = productRepository.findDetailedBySlug(slug).filter(productStatusPolicy::isPubliclyVisible)
                .orElseThrow(() -> new AppException(ProductErrorCode.PRODUCT_NOT_FOUND));
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAdminProducts(ProductStatus status, Long categoryId, String keyword,
            int page, int size) {
        var productPage = productRepository.findAll(productSpecifications.adminProducts(status, categoryId, keyword),
                PageRequestUtils.newestFirst(page, size)).map(productMapper::toResponse);
        return PageResponse.from(productPage);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getAdminProduct(Long productId) {
        return productMapper.toResponse(findDetailedProduct(productId));
    }

    @Override
    @Transactional
    public ProductResponse createAdminProduct(ProductCreateRequest request) {
        Category category = findVisibleCategory(request.getCategoryId());
        validateProductSku(request.getSku(), null);
        String slug = resolveProductSlug(request.getSlug(), request.getProductName());
        validateProductSlug(slug, null);
        validateBookAuthor(request.getBookAuthor());
        String isbn = trimToNull(request.getIsbn());
        validateIsbn(isbn, null);
        validatePublicationYear(request.getPublicationYear());
        validatePageCount(request.getPageCount());

        Product product = Product.builder().category(category).sku(request.getSku().trim())
                .productName(request.getProductName().trim()).slug(slug).description(request.getDescription().trim())
                .brand(trimToNull(request.getBrand())).material(trimToNull(request.getMaterial()))
                .bookAuthor(request.getBookAuthor().trim()).isbn(isbn).publisher(trimToNull(request.getPublisher()))
                .publicationYear(request.getPublicationYear()).bookLanguage(trimToNull(request.getBookLanguage()))
                .pageCount(request.getPageCount()).bookFormat(resolveBookFormat(request.getBookFormat()))
                .dimensions(trimToNull(request.getDimensions())).price(request.getPrice())
                .originalPrice(request.getOriginalPrice()).discountPercentage(request.getDiscountPercentage())
                .weight(request.getWeight()).active(true).featured(false).status(ProductStatus.ACTIVE)
                .approvedBy(getCurrentUserId()).approvedAt(Instant.now()).build();

        for (ProductVariationRequest variationRequest : request.getVariations()) {
            validateVariationSku(variationRequest.getSku(), null);
            ProductVariation variation = buildVariation(variationRequest, product);
            product.getProductVariations().add(variation);
        }
        recalculateStock(product);
        productStatusPolicy.requireActiveVariation(product);

        Product savedProduct = productRepository.save(product);
        eventPublisher.publishEvent(new ProductCatalogChangedEvent(savedProduct.getId()));
        log.info("Admin created product {}", savedProduct.getId());
        return productMapper.toResponse(savedProduct);
    }

    @Override
    @Transactional
    public ProductResponse updateAdminProduct(Long productId, ProductUpdateRequest request) {
        Product product = findDetailedProduct(productId);

        if (StringUtils.hasText(request.getSku())) {
            validateProductSku(request.getSku(), productId);
            product.setSku(request.getSku().trim());
        }
        if (StringUtils.hasText(request.getProductName())) {
            product.setProductName(request.getProductName().trim());
        }
        if (StringUtils.hasText(request.getSlug()) || StringUtils.hasText(request.getProductName())) {
            String slug = resolveProductSlug(request.getSlug(), product.getProductName());
            validateProductSlug(slug, productId);
            product.setSlug(slug);
        }
        if (StringUtils.hasText(request.getDescription())) {
            product.setDescription(request.getDescription().trim());
        }
        if (request.getBrand() != null) {
            product.setBrand(trimToNull(request.getBrand()));
        }
        if (request.getMaterial() != null) {
            product.setMaterial(trimToNull(request.getMaterial()));
        }
        if (request.getBookAuthor() != null) {
            validateBookAuthor(request.getBookAuthor());
            product.setBookAuthor(request.getBookAuthor().trim());
        }
        if (request.getIsbn() != null) {
            String isbn = trimToNull(request.getIsbn());
            validateIsbn(isbn, productId);
            product.setIsbn(isbn);
        }
        if (request.getPublisher() != null) {
            product.setPublisher(trimToNull(request.getPublisher()));
        }
        if (request.getPublicationYear() != null) {
            validatePublicationYear(request.getPublicationYear());
            product.setPublicationYear(request.getPublicationYear());
        }
        if (request.getBookLanguage() != null) {
            product.setBookLanguage(trimToNull(request.getBookLanguage()));
        }
        if (request.getPageCount() != null) {
            validatePageCount(request.getPageCount());
            product.setPageCount(request.getPageCount());
        }
        if (request.getBookFormat() != null) {
            product.setBookFormat(request.getBookFormat());
        }
        if (request.getDimensions() != null) {
            product.setDimensions(trimToNull(request.getDimensions()));
        }
        if (request.getCategoryId() != null) {
            product.setCategory(findVisibleCategory(request.getCategoryId()));
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getOriginalPrice() != null) {
            product.setOriginalPrice(request.getOriginalPrice());
        }
        if (request.getDiscountPercentage() != null) {
            product.setDiscountPercentage(request.getDiscountPercentage());
        }
        if (request.getWeight() != null) {
            product.setWeight(request.getWeight());
        }
        if (request.getFeatured() != null) {
            product.setFeatured(request.getFeatured());
        }

        Product saved = productRepository.save(product);
        eventPublisher.publishEvent(new ProductCatalogChangedEvent(saved.getId()));
        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteAdminProduct(Long productId) {
        Product product = findDetailedProduct(productId);
        product.setStatus(ProductStatus.INACTIVE);
        product.setActive(false);
        productRepository.save(product);
        eventPublisher.publishEvent(new ProductCatalogChangedEvent(productId));
    }

    @Override
    @Transactional
    public ProductResponse uploadProductMedia(Long productId, MultipartFile mediaFile, String altText,
            Integer sortOrder, Boolean primary) {
        Product product = findDetailedProduct(productId);

        fileValidatorService.validateFile(mediaFile, MediaType.IMAGE);
        CloudinaryUploadResult uploadResult = cloudinaryStorageService.uploadImage(mediaFile,
                cloudinaryProperties.getProductMediaFolder() + "/" + product.getId(), "product-" + product.getId(),
                PRODUCT_MEDIA_WIDTH, PRODUCT_MEDIA_HEIGHT);

        ProductMedia media = ProductMedia.builder().product(product).mediaUrl(uploadResult.secureUrl())
                .mediaPublicId(uploadResult.publicId()).mediaType(MediaType.IMAGE).altText(trimToNull(altText))
                .sortOrder(sortOrder == null ? nextMediaSortOrder(product) : sortOrder)
                .primary(Boolean.TRUE.equals(primary)).active(true).build();
        if (Boolean.TRUE.equals(media.getPrimary())) {
            unsetPrimaryMedia(product);
            product.setThumbnailUrl(media.getMediaUrl());
            product.setThumbnailPublicId(media.getMediaPublicId());
        }
        product.getProductMedia().add(media);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse updateProductMedia(Long productId, Long mediaId, ProductMediaUpdateRequest request) {
        Product product = findDetailedProduct(productId);
        ProductMedia media = findProductMedia(productId, mediaId);

        if (request.getAltText() != null) {
            media.setAltText(trimToNull(request.getAltText()));
        }
        if (request.getSortOrder() != null) {
            media.setSortOrder(request.getSortOrder());
        }
        if (request.getActive() != null) {
            media.setActive(request.getActive());
        }
        if (Boolean.TRUE.equals(request.getPrimary())) {
            unsetPrimaryMedia(product);
            media.setPrimary(true);
            product.setThumbnailUrl(media.getMediaUrl());
            product.setThumbnailPublicId(media.getMediaPublicId());
        } else if (Boolean.FALSE.equals(request.getPrimary()) && Boolean.TRUE.equals(media.getPrimary())) {
            media.setPrimary(false);
            product.setThumbnailUrl(null);
            product.setThumbnailPublicId(null);
        }

        mediaRepository.save(media);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public void deleteProductMedia(Long productId, Long mediaId) {
        Product product = findDetailedProduct(productId);
        ProductMedia media = findProductMedia(productId, mediaId);
        media.setActive(false);
        if (Boolean.TRUE.equals(media.getPrimary())) {
            media.setPrimary(false);
            product.setThumbnailUrl(null);
            product.setThumbnailPublicId(null);
        }
        mediaRepository.save(media);
        productRepository.save(product);
    }

    @Override
    @Transactional
    public ProductResponse createVariation(Long productId, ProductVariationRequest request) {
        Product product = findDetailedProduct(productId);
        validateVariationSku(request.getSku(), null);
        product.getProductVariations().add(buildVariation(request, product));
        recalculateStock(product);
        Product saved = productRepository.save(product);
        eventPublisher.publishEvent(new ProductCatalogChangedEvent(productId));
        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse updateVariation(Long productId, Long variationId, ProductVariationRequest request) {
        Product product = findDetailedProduct(productId);
        ProductVariation variation = findProductVariation(productId, variationId);
        validateVariationSku(request.getSku(), variationId);
        applyVariation(request, variation);
        recalculateStock(product);
        productStatusPolicy.requireActiveVariation(product);
        variationRepository.save(variation);
        Product saved = productRepository.save(product);
        eventPublisher.publishEvent(new ProductCatalogChangedEvent(productId));
        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteVariation(Long productId, Long variationId) {
        Product product = findDetailedProduct(productId);
        ProductVariation variation = findProductVariation(productId, variationId);
        variation.setActive(false);
        recalculateStock(product);
        productStatusPolicy.requireActiveVariation(product);
        variationRepository.save(variation);
        productRepository.save(product);
        eventPublisher.publishEvent(new ProductCatalogChangedEvent(productId));
    }

    @Override
    @Transactional
    public ProductResponse updateVariationStock(Long productId, Long variationId, StockUpdateRequest request) {
        Product product = findDetailedProduct(productId);
        ProductVariation variation = findProductVariation(productId, variationId);
        variation.setStockQuantity(request.getStockQuantity());
        recalculateStock(product);
        variationRepository.save(variation);
        Product saved = productRepository.save(product);
        eventPublisher.publishEvent(new ProductCatalogChangedEvent(productId));
        return productMapper.toResponse(saved);
    }

    private Product findDetailedProduct(Long productId) {
        return productRepository.findDetailedById(productId)
                .orElseThrow(() -> new AppException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }

    private Category findVisibleCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(CategoryErrorCode.CATEGORY_NOT_FOUND));
        if (!Boolean.TRUE.equals(category.getActive()) || !Boolean.TRUE.equals(category.getVisible())) {
            throw new AppException(CategoryErrorCode.CATEGORY_NOT_FOUND);
        }
        return category;
    }

    private ProductVariation findProductVariation(Long productId, Long variationId) {
        return variationRepository.findByIdAndProductId(variationId, productId)
                .orElseThrow(() -> new AppException(ProductErrorCode.PRODUCT_VARIATION_NOT_FOUND));
    }

    private ProductMedia findProductMedia(Long productId, Long mediaId) {
        return mediaRepository.findByIdAndProductId(mediaId, productId)
                .orElseThrow(() -> new AppException(ProductErrorCode.PRODUCT_MEDIA_NOT_FOUND));
    }

    private ProductVariation buildVariation(ProductVariationRequest request, Product product) {
        ProductVariation variation = new ProductVariation();
        variation.setProduct(product);
        applyVariation(request, variation);
        return variation;
    }

    private void applyVariation(ProductVariationRequest request, ProductVariation variation) {
        variation.setSku(request.getSku().trim());
        variation.setColor(request.getColor().trim());
        variation.setSize(request.getSize().trim());
        variation.setAdditionalPrice(request.getAdditionalPrice());
        variation.setStockQuantity(request.getStockQuantity());
        variation.setImageUrl(trimToNull(request.getImageUrl()));
        variation.setImagePublicId(trimToNull(request.getImagePublicId()));
        variation.setActive(request.getActive() == null ? true : request.getActive());
    }

    private void validateProductSku(String sku, Long productId) {
        String trimmed = sku.trim();
        boolean exists = productId == null ? productRepository.existsBySku(trimmed)
                : productRepository.existsBySkuAndIdNot(trimmed, productId);
        if (exists) {
            throw new AppException(ProductErrorCode.PRODUCT_SKU_ALREADY_EXISTS);
        }
    }

    private void validateProductSlug(String slug, Long productId) {
        boolean exists = productId == null ? productRepository.existsBySlug(slug)
                : productRepository.existsBySlugAndIdNot(slug, productId);
        if (exists) {
            throw new AppException(ProductErrorCode.PRODUCT_SLUG_ALREADY_EXISTS);
        }
    }

    private void validateVariationSku(String sku, Long variationId) {
        String trimmed = sku.trim();
        boolean exists = variationId == null ? variationRepository.existsBySku(trimmed)
                : variationRepository.existsBySkuAndIdNot(trimmed, variationId);
        if (exists) {
            throw new AppException(ProductErrorCode.PRODUCT_VARIATION_SKU_ALREADY_EXISTS);
        }
    }

    private void validateBookAuthor(String bookAuthor) {
        if (!StringUtils.hasText(bookAuthor)) {
            throw new AppException(ProductErrorCode.PRODUCT_AUTHOR_REQUIRED);
        }
    }

    private void validateIsbn(String isbn, Long productId) {
        if (!StringUtils.hasText(isbn)) {
            return;
        }
        boolean exists = productId == null ? productRepository.existsByIsbn(isbn)
                : productRepository.existsByIsbnAndIdNot(isbn, productId);
        if (exists) {
            throw new AppException(ProductErrorCode.PRODUCT_ISBN_ALREADY_EXISTS);
        }
    }

    private void validatePublicationYear(Integer publicationYear) {
        if (publicationYear == null) {
            return;
        }
        int maxYear = Year.now().getValue() + 1;
        if (publicationYear < 1000 || publicationYear > maxYear) {
            throw new AppException(ProductErrorCode.PRODUCT_INVALID_PUBLICATION_YEAR);
        }
    }

    private void validatePageCount(Integer pageCount) {
        if (pageCount != null && pageCount <= 0) {
            throw new AppException(ProductErrorCode.PRODUCT_INVALID_PAGE_COUNT);
        }
    }

    private BookFormat resolveBookFormat(BookFormat bookFormat) {
        return bookFormat == null ? BookFormat.PAPERBACK : bookFormat;
    }

    private String resolveProductSlug(String requestedSlug, String productName) {
        return SlugUtils.slugify(StringUtils.hasText(requestedSlug) ? requestedSlug : productName, "product");
    }

    private void recalculateStock(Product product) {
        int totalStock = product.getProductVariations().stream()
                .filter(variation -> Boolean.TRUE.equals(variation.getActive())).map(ProductVariation::getStockQuantity)
                .filter(stock -> stock != null).mapToInt(Integer::intValue).sum();
        product.setStockQuantity(totalStock);
    }

    private void unsetPrimaryMedia(Product product) {
        product.getProductMedia().forEach(media -> media.setPrimary(false));
    }

    private int nextMediaSortOrder(Product product) {
        return product.getProductMedia().stream().map(ProductMedia::getSortOrder).filter(sortOrder -> sortOrder != null)
                .mapToInt(Integer::intValue).max().orElse(-1) + 1;
    }

    private Sort resolvePublicSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return switch (sort.trim().toLowerCase(Locale.ROOT)) {
        case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
        case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
        case "best_selling" -> Sort.by(Sort.Direction.DESC, "soldCount");
        case "name_asc" -> Sort.by(Sort.Direction.ASC, "productName");
        case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
        default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private String getCurrentUserId() {
        return currentUserService.getCurrentUserId();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
