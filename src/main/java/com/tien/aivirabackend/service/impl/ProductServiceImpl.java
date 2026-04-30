package com.tien.aivirabackend.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.tien.aivirabackend.config.properties.CloudinaryProperties;
import com.tien.aivirabackend.constant.MediaType;
import com.tien.aivirabackend.constant.ProductStatus;
import com.tien.aivirabackend.constant.ShopStatus;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.ProductCreateRequest;
import com.tien.aivirabackend.domain.dto.request.ProductMediaUpdateRequest;
import com.tien.aivirabackend.domain.dto.request.ProductUpdateRequest;
import com.tien.aivirabackend.domain.dto.request.ProductVariationRequest;
import com.tien.aivirabackend.domain.dto.request.ShopModerationRequest;
import com.tien.aivirabackend.domain.dto.request.StockUpdateRequest;
import com.tien.aivirabackend.domain.dto.response.ProductResponse;
import com.tien.aivirabackend.domain.entity.catalog.Category;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.entity.catalog.ProductMedia;
import com.tien.aivirabackend.domain.entity.catalog.ProductVariation;
import com.tien.aivirabackend.domain.entity.marketplace.Shop;
import com.tien.aivirabackend.domain.mapper.ProductMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.CategoryErrorCode;
import com.tien.aivirabackend.exception.errorCode.ProductErrorCode;
import com.tien.aivirabackend.repository.CategoryRepository;
import com.tien.aivirabackend.repository.ProductMediaRepository;
import com.tien.aivirabackend.repository.ProductRepository;
import com.tien.aivirabackend.repository.ProductVariationRepository;
import com.tien.aivirabackend.service.CloudinaryStorageService;
import com.tien.aivirabackend.service.CloudinaryUploadResult;
import com.tien.aivirabackend.service.FileValidatorService;
import com.tien.aivirabackend.service.ProductService;
import com.tien.aivirabackend.service.ShopOwnershipService;
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
    ShopOwnershipService shopOwnershipService;
    ProductMapper productMapper;
    FileValidatorService fileValidatorService;
    CloudinaryStorageService cloudinaryStorageService;
    CloudinaryProperties cloudinaryProperties;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getPublicProducts(
            String keyword,
            String categorySlug,
            String shopSlug,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean available,
            String sort,
            int page,
            int size) {
        var productPage = productRepository
                .findAll(
                        buildPublicProductSpecification(
                                keyword, categorySlug, shopSlug, brand, minPrice, maxPrice, available),
                        pageRequest(page, size, resolvePublicSort(sort)))
                .map(productMapper::toResponse);
        return PageResponse.from(productPage);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getPublicProduct(String slug) {
        Product product = productRepository
                .findDetailedBySlug(slug)
                .filter(this::isPubliclyVisible)
                .orElseThrow(() -> new AppException(ProductErrorCode.PRODUCT_NOT_FOUND));
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getSellerProducts(ProductStatus status, String keyword, int page, int size) {
        Shop shop = shopOwnershipService.requireCurrentUserApprovedShop();
        var productPage = productRepository
                .findAll(buildSellerProductSpecification(shop.getId(), status, keyword), pageRequest(page, size))
                .map(productMapper::toResponse);
        return PageResponse.from(productPage);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getSellerProduct(Long productId) {
        Shop shop = shopOwnershipService.requireCurrentUserApprovedShop();
        Product product = findDetailedProduct(productId);
        requireProductOwner(product, shop);
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse createSellerProduct(ProductCreateRequest request) {
        Shop shop = shopOwnershipService.requireCurrentUserApprovedShop();
        Category category = findVisibleCategory(request.getCategoryId());
        validateProductSku(request.getSku(), null);
        String slug = resolveProductSlug(request.getSlug(), request.getProductName());
        validateProductSlug(slug, null);

        Product product = Product.builder()
                .shop(shop)
                .category(category)
                .sku(request.getSku().trim())
                .productName(request.getProductName().trim())
                .slug(slug)
                .description(request.getDescription().trim())
                .brand(trimToNull(request.getBrand()))
                .material(trimToNull(request.getMaterial()))
                .price(request.getPrice())
                .originalPrice(request.getOriginalPrice())
                .discountPercentage(request.getDiscountPercentage())
                .weight(request.getWeight())
                .active(true)
                .featured(false)
                .status(ProductStatus.DRAFT)
                .build();

        for (ProductVariationRequest variationRequest : request.getVariations()) {
            validateVariationSku(variationRequest.getSku(), null);
            ProductVariation variation = buildVariation(variationRequest, product);
            product.getProductVariations().add(variation);
        }
        recalculateStock(product);

        Product savedProduct = productRepository.save(product);
        log.info("Seller shop {} created product {}", shop.getId(), savedProduct.getId());
        return productMapper.toResponse(savedProduct);
    }

    @Override
    @Transactional
    public ProductResponse updateSellerProduct(Long productId, ProductUpdateRequest request) {
        Shop shop = shopOwnershipService.requireCurrentUserApprovedShop();
        Product product = findDetailedProduct(productId);
        requireProductOwner(product, shop);

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

        moveEditableProductToDraft(product);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public void deleteSellerProduct(Long productId) {
        Shop shop = shopOwnershipService.requireCurrentUserApprovedShop();
        Product product = findDetailedProduct(productId);
        requireProductOwner(product, shop);
        product.setStatus(ProductStatus.INACTIVE);
        product.setActive(false);
        productRepository.save(product);
    }

    @Override
    @Transactional
    public ProductResponse submitSellerProduct(Long productId) {
        Shop shop = shopOwnershipService.requireCurrentUserApprovedShop();
        Product product = findDetailedProduct(productId);
        requireProductOwner(product, shop);
        if (product.getStatus() != ProductStatus.DRAFT && product.getStatus() != ProductStatus.REJECTED) {
            throw new AppException(ProductErrorCode.PRODUCT_INVALID_STATUS_TRANSITION);
        }
        requireActiveVariation(product);

        product.setStatus(ProductStatus.PENDING_REVIEW);
        product.setActive(true);
        product.setSubmittedAt(Instant.now());
        product.setRejectionReason(null);
        product.setRejectedAt(null);
        product.setRejectedBy(null);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse uploadProductMedia(
            Long productId, MultipartFile mediaFile, String altText, Integer sortOrder, Boolean primary) {
        Shop shop = shopOwnershipService.requireCurrentUserApprovedShop();
        Product product = findDetailedProduct(productId);
        requireProductOwner(product, shop);

        fileValidatorService.validateFile(mediaFile, MediaType.IMAGE);
        CloudinaryUploadResult uploadResult = cloudinaryStorageService.uploadImage(
                mediaFile,
                cloudinaryProperties.getProductMediaFolder() + "/" + shop.getId() + "/" + product.getId(),
                "product-" + product.getId(),
                PRODUCT_MEDIA_WIDTH,
                PRODUCT_MEDIA_HEIGHT);

        ProductMedia media = ProductMedia.builder()
                .product(product)
                .mediaUrl(uploadResult.secureUrl())
                .mediaPublicId(uploadResult.publicId())
                .mediaType(MediaType.IMAGE)
                .altText(trimToNull(altText))
                .sortOrder(sortOrder == null ? nextMediaSortOrder(product) : sortOrder)
                .primary(Boolean.TRUE.equals(primary))
                .active(true)
                .build();
        if (Boolean.TRUE.equals(media.getPrimary())) {
            unsetPrimaryMedia(product);
            product.setThumbnailUrl(media.getMediaUrl());
            product.setThumbnailPublicId(media.getMediaPublicId());
        }
        product.getProductMedia().add(media);
        moveEditableProductToDraft(product);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse updateProductMedia(Long productId, Long mediaId, ProductMediaUpdateRequest request) {
        Shop shop = shopOwnershipService.requireCurrentUserApprovedShop();
        Product product = findDetailedProduct(productId);
        requireProductOwner(product, shop);
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

        moveEditableProductToDraft(product);
        mediaRepository.save(media);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public void deleteProductMedia(Long productId, Long mediaId) {
        Shop shop = shopOwnershipService.requireCurrentUserApprovedShop();
        Product product = findDetailedProduct(productId);
        requireProductOwner(product, shop);
        ProductMedia media = findProductMedia(productId, mediaId);
        media.setActive(false);
        if (Boolean.TRUE.equals(media.getPrimary())) {
            media.setPrimary(false);
            product.setThumbnailUrl(null);
            product.setThumbnailPublicId(null);
        }
        moveEditableProductToDraft(product);
        mediaRepository.save(media);
        productRepository.save(product);
    }

    @Override
    @Transactional
    public ProductResponse createVariation(Long productId, ProductVariationRequest request) {
        Shop shop = shopOwnershipService.requireCurrentUserApprovedShop();
        Product product = findDetailedProduct(productId);
        requireProductOwner(product, shop);
        validateVariationSku(request.getSku(), null);
        product.getProductVariations().add(buildVariation(request, product));
        recalculateStock(product);
        moveEditableProductToDraft(product);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse updateVariation(Long productId, Long variationId, ProductVariationRequest request) {
        Shop shop = shopOwnershipService.requireCurrentUserApprovedShop();
        Product product = findDetailedProduct(productId);
        requireProductOwner(product, shop);
        ProductVariation variation = findProductVariation(productId, variationId);
        validateVariationSku(request.getSku(), variationId);
        applyVariation(request, variation);
        recalculateStock(product);
        requireActiveVariation(product);
        moveEditableProductToDraft(product);
        variationRepository.save(variation);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public void deleteVariation(Long productId, Long variationId) {
        Shop shop = shopOwnershipService.requireCurrentUserApprovedShop();
        Product product = findDetailedProduct(productId);
        requireProductOwner(product, shop);
        ProductVariation variation = findProductVariation(productId, variationId);
        variation.setActive(false);
        recalculateStock(product);
        requireActiveVariation(product);
        moveEditableProductToDraft(product);
        variationRepository.save(variation);
        productRepository.save(product);
    }

    @Override
    @Transactional
    public ProductResponse updateVariationStock(Long productId, Long variationId, StockUpdateRequest request) {
        Shop shop = shopOwnershipService.requireCurrentUserApprovedShop();
        Product product = findDetailedProduct(productId);
        requireProductOwner(product, shop);
        ProductVariation variation = findProductVariation(productId, variationId);
        variation.setStockQuantity(request.getStockQuantity());
        recalculateStock(product);
        variationRepository.save(variation);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAdminProducts(
            ProductStatus status, Long shopId, Long categoryId, String keyword, int page, int size) {
        var productPage = productRepository
                .findAll(buildAdminProductSpecification(status, shopId, categoryId, keyword), pageRequest(page, size))
                .map(productMapper::toResponse);
        return PageResponse.from(productPage);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getAdminProduct(Long productId) {
        return productMapper.toResponse(findDetailedProduct(productId));
    }

    @Override
    @Transactional
    public ProductResponse approve(Long productId) {
        Product product = findDetailedProduct(productId);
        if (product.getStatus() != ProductStatus.PENDING_REVIEW) {
            throw new AppException(ProductErrorCode.PRODUCT_INVALID_STATUS_TRANSITION);
        }
        requireActiveVariation(product);
        product.setStatus(ProductStatus.ACTIVE);
        product.setActive(true);
        product.setApprovedAt(Instant.now());
        product.setApprovedBy(getCurrentUserId());
        product.setRejectionReason(null);
        product.setRejectedAt(null);
        product.setRejectedBy(null);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse reject(Long productId, ShopModerationRequest request) {
        Product product = findDetailedProduct(productId);
        if (product.getStatus() != ProductStatus.PENDING_REVIEW) {
            throw new AppException(ProductErrorCode.PRODUCT_INVALID_STATUS_TRANSITION);
        }
        product.setStatus(ProductStatus.REJECTED);
        product.setRejectionReason(request.getReason().trim());
        product.setRejectedAt(Instant.now());
        product.setRejectedBy(getCurrentUserId());
        return productMapper.toResponse(productRepository.save(product));
    }

    private Product findDetailedProduct(Long productId) {
        return productRepository
                .findDetailedById(productId)
                .orElseThrow(() -> new AppException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }

    private Category findVisibleCategory(Long categoryId) {
        Category category = categoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new AppException(CategoryErrorCode.CATEGORY_NOT_FOUND));
        if (!Boolean.TRUE.equals(category.getActive()) || !Boolean.TRUE.equals(category.getVisible())) {
            throw new AppException(CategoryErrorCode.CATEGORY_NOT_FOUND);
        }
        return category;
    }

    private ProductVariation findProductVariation(Long productId, Long variationId) {
        return variationRepository
                .findByIdAndProductId(variationId, productId)
                .orElseThrow(() -> new AppException(ProductErrorCode.PRODUCT_VARIATION_NOT_FOUND));
    }

    private ProductMedia findProductMedia(Long productId, Long mediaId) {
        return mediaRepository
                .findByIdAndProductId(mediaId, productId)
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

    private void requireProductOwner(Product product, Shop shop) {
        if (product.getShop() == null || !product.getShop().getId().equals(shop.getId())) {
            throw new AppException(ProductErrorCode.PRODUCT_NOT_OWNER);
        }
    }

    private void validateProductSku(String sku, Long productId) {
        String trimmed = sku.trim();
        boolean exists = productId == null
                ? productRepository.existsBySku(trimmed)
                : productRepository.existsBySkuAndIdNot(trimmed, productId);
        if (exists) {
            throw new AppException(ProductErrorCode.PRODUCT_SKU_ALREADY_EXISTS);
        }
    }

    private void validateProductSlug(String slug, Long productId) {
        boolean exists = productId == null
                ? productRepository.existsBySlug(slug)
                : productRepository.existsBySlugAndIdNot(slug, productId);
        if (exists) {
            throw new AppException(ProductErrorCode.PRODUCT_SLUG_ALREADY_EXISTS);
        }
    }

    private void validateVariationSku(String sku, Long variationId) {
        String trimmed = sku.trim();
        boolean exists = variationId == null
                ? variationRepository.existsBySku(trimmed)
                : variationRepository.existsBySkuAndIdNot(trimmed, variationId);
        if (exists) {
            throw new AppException(ProductErrorCode.PRODUCT_VARIATION_SKU_ALREADY_EXISTS);
        }
    }

    private String resolveProductSlug(String requestedSlug, String productName) {
        return SlugUtils.slugify(StringUtils.hasText(requestedSlug) ? requestedSlug : productName, "product");
    }

    private void recalculateStock(Product product) {
        int totalStock = product.getProductVariations().stream()
                .filter(variation -> Boolean.TRUE.equals(variation.getActive()))
                .map(ProductVariation::getStockQuantity)
                .filter(stock -> stock != null)
                .mapToInt(Integer::intValue)
                .sum();
        product.setStockQuantity(totalStock);
    }

    private void requireActiveVariation(Product product) {
        boolean hasActiveVariation = product.getProductVariations().stream()
                .anyMatch(variation -> Boolean.TRUE.equals(variation.getActive()));
        if (!hasActiveVariation) {
            throw new AppException(ProductErrorCode.PRODUCT_VARIATION_REQUIRED);
        }
    }

    private void moveEditableProductToDraft(Product product) {
        if (product.getStatus() == ProductStatus.ACTIVE
                || product.getStatus() == ProductStatus.PENDING_REVIEW
                || product.getStatus() == ProductStatus.REJECTED) {
            product.setStatus(ProductStatus.DRAFT);
            product.setSubmittedAt(null);
            product.setRejectionReason(null);
            product.setRejectedAt(null);
            product.setRejectedBy(null);
            product.setApprovedAt(null);
            product.setApprovedBy(null);
        }
        product.setActive(product.getStatus() != ProductStatus.INACTIVE);
    }

    private void unsetPrimaryMedia(Product product) {
        product.getProductMedia().forEach(media -> media.setPrimary(false));
    }

    private int nextMediaSortOrder(Product product) {
        return product.getProductMedia().stream()
                        .map(ProductMedia::getSortOrder)
                        .filter(sortOrder -> sortOrder != null)
                        .mapToInt(Integer::intValue)
                        .max()
                        .orElse(-1)
                + 1;
    }

    private boolean isPubliclyVisible(Product product) {
        return product.getStatus() == ProductStatus.ACTIVE
                && Boolean.TRUE.equals(product.getActive())
                && product.getShop() != null
                && product.getShop().getStatus() == ShopStatus.APPROVED
                && product.getCategory() != null
                && Boolean.TRUE.equals(product.getCategory().getActive())
                && Boolean.TRUE.equals(product.getCategory().getVisible());
    }

    private Specification<Product> buildPublicProductSpecification(
            String keyword,
            String categorySlug,
            String shopSlug,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean available) {
        return (root, query, cb) -> {
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("shop", JoinType.LEFT);
                root.fetch("category", JoinType.LEFT);
                root.fetch("productVariations", JoinType.LEFT);
                root.fetch("productMedia", JoinType.LEFT);
                query.distinct(true);
            }
            Join<Product, Category> category = root.join("category");
            Join<Product, Shop> shop = root.join("shop");
            Predicate predicate = cb.conjunction();
            predicate = cb.and(predicate, cb.equal(root.get("status"), ProductStatus.ACTIVE));
            predicate = cb.and(predicate, cb.isTrue(root.get("active")));
            predicate = cb.and(predicate, cb.isTrue(category.get("active")), cb.isTrue(category.get("visible")));
            predicate = cb.and(predicate, cb.equal(shop.get("status"), ShopStatus.APPROVED));
            predicate = addCommonFilters(predicate, root, category, shop, cb, keyword, categorySlug, shopSlug, brand);
            if (minPrice != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            if (available != null) {
                predicate = Boolean.TRUE.equals(available)
                        ? cb.and(predicate, cb.greaterThan(root.get("stockQuantity"), 0))
                        : cb.and(predicate, cb.lessThanOrEqualTo(root.get("stockQuantity"), 0));
            }
            return predicate;
        };
    }

    private Specification<Product> buildSellerProductSpecification(Long shopId, ProductStatus status, String keyword) {
        return (root, query, cb) -> {
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("shop", JoinType.LEFT);
                root.fetch("category", JoinType.LEFT);
                root.fetch("productVariations", JoinType.LEFT);
                root.fetch("productMedia", JoinType.LEFT);
                query.distinct(true);
            }
            Predicate predicate = cb.equal(root.get("shop").get("id"), shopId);
            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(keyword)) {
                predicate = cb.and(predicate, keywordPredicate(root, cb, keyword));
            }
            return predicate;
        };
    }

    private Specification<Product> buildAdminProductSpecification(
            ProductStatus status, Long shopId, Long categoryId, String keyword) {
        return (root, query, cb) -> {
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("shop", JoinType.LEFT);
                root.fetch("category", JoinType.LEFT);
                root.fetch("productVariations", JoinType.LEFT);
                root.fetch("productMedia", JoinType.LEFT);
                query.distinct(true);
            }
            Predicate predicate = cb.conjunction();
            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            if (shopId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("shop").get("id"), shopId));
            }
            if (categoryId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("category").get("id"), categoryId));
            }
            if (StringUtils.hasText(keyword)) {
                predicate = cb.and(predicate, keywordPredicate(root, cb, keyword));
            }
            return predicate;
        };
    }

    private Predicate addCommonFilters(
            Predicate predicate,
            jakarta.persistence.criteria.Root<Product> root,
            Join<Product, Category> category,
            Join<Product, Shop> shop,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            String keyword,
            String categorySlug,
            String shopSlug,
            String brand) {
        if (StringUtils.hasText(keyword)) {
            predicate = cb.and(predicate, keywordPredicate(root, cb, keyword));
        }
        if (StringUtils.hasText(categorySlug)) {
            predicate = cb.and(predicate, cb.equal(category.get("slug"), categorySlug.trim()));
        }
        if (StringUtils.hasText(shopSlug)) {
            predicate = cb.and(predicate, cb.equal(shop.get("slug"), shopSlug.trim()));
        }
        if (StringUtils.hasText(brand)) {
            predicate = cb.and(
                    predicate,
                    cb.equal(cb.lower(root.get("brand")), brand.trim().toLowerCase(Locale.ROOT)));
        }
        return predicate;
    }

    private Predicate keywordPredicate(
            jakarta.persistence.criteria.Root<Product> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            String keyword) {
        String likeKeyword = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        return cb.or(
                cb.like(cb.lower(root.get("productName")), likeKeyword),
                cb.like(cb.lower(root.get("sku")), likeKeyword),
                cb.like(cb.lower(root.get("description")), likeKeyword),
                cb.like(cb.lower(root.get("brand")), likeKeyword));
    }

    private PageRequest pageRequest(int page, int size) {
        return pageRequest(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private PageRequest pageRequest(int page, int size, Sort sort) {
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(pageIndex, pageSize, sort);
    }

    private Sort resolvePublicSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return switch (sort.trim().toLowerCase(Locale.ROOT)) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "best_selling" -> Sort.by(Sort.Direction.DESC, "soldCount");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private String getCurrentUserId() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(com.tien.aivirabackend.exception.errorCode.AuthErrorCode.AUTHENTICATION_FAILED);
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof org.springframework.security.oauth2.jwt.Jwt jwt)) {
            throw new AppException(com.tien.aivirabackend.exception.errorCode.AuthErrorCode.AUTHENTICATION_FAILED);
        }
        String userId = jwt.getClaimAsString("user_id");
        if (!StringUtils.hasText(userId)) {
            throw new AppException(com.tien.aivirabackend.exception.errorCode.AuthErrorCode.AUTHENTICATION_FAILED);
        }
        return userId;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
