package com.tien.aivirabackend.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.tien.aivirabackend.config.properties.CloudinaryProperties;
import com.tien.aivirabackend.constant.MediaType;
import com.tien.aivirabackend.constant.ProductStatus;
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
import com.tien.aivirabackend.service.CurrentUserService;
import com.tien.aivirabackend.service.FileValidatorService;
import com.tien.aivirabackend.service.ProductService;
import com.tien.aivirabackend.service.ShopOwnershipService;
import com.tien.aivirabackend.service.product.ProductSpecifications;
import com.tien.aivirabackend.service.product.ProductStatusPolicy;
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
    ShopOwnershipService shopOwnershipService;
    ProductMapper productMapper;
    FileValidatorService fileValidatorService;
    CloudinaryStorageService cloudinaryStorageService;
    CloudinaryProperties cloudinaryProperties;
    CurrentUserService currentUserService;
    ProductSpecifications productSpecifications;
    ProductStatusPolicy productStatusPolicy;

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
                        productSpecifications.publicProducts(
                                keyword, categorySlug, shopSlug, brand, minPrice, maxPrice, available),
                        PageRequestUtils.of(page, size, resolvePublicSort(sort)))
                .map(productMapper::toResponse);
        return PageResponse.from(productPage);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getPublicProduct(String slug) {
        Product product = productRepository
                .findDetailedBySlug(slug)
                .filter(productStatusPolicy::isPubliclyVisible)
                .orElseThrow(() -> new AppException(ProductErrorCode.PRODUCT_NOT_FOUND));
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getSellerProducts(ProductStatus status, String keyword, int page, int size) {
        Shop shop = shopOwnershipService.requireCurrentUserApprovedShop();
        var productPage = productRepository
                .findAll(
                        productSpecifications.sellerProducts(shop.getId(), status, keyword),
                        PageRequestUtils.newestFirst(page, size))
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

        productStatusPolicy.moveEditableProductToDraft(product);
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
        productStatusPolicy.requireActiveVariation(product);

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
        productStatusPolicy.moveEditableProductToDraft(product);
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

        productStatusPolicy.moveEditableProductToDraft(product);
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
        productStatusPolicy.moveEditableProductToDraft(product);
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
        productStatusPolicy.moveEditableProductToDraft(product);
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
        productStatusPolicy.requireActiveVariation(product);
        productStatusPolicy.moveEditableProductToDraft(product);
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
        productStatusPolicy.requireActiveVariation(product);
        productStatusPolicy.moveEditableProductToDraft(product);
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
                .findAll(
                        productSpecifications.adminProducts(status, shopId, categoryId, keyword),
                        PageRequestUtils.newestFirst(page, size))
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
        productStatusPolicy.requireActiveVariation(product);
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
