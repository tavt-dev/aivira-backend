package com.tien.aivirabackend.service.catalog.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tien.aivirabackend.config.properties.CloudinaryProperties;
import com.tien.aivirabackend.constant.ProductStatus;
import com.tien.aivirabackend.domain.dto.request.ProductCreateRequest;
import com.tien.aivirabackend.domain.dto.request.ProductUpdateRequest;
import com.tien.aivirabackend.domain.dto.request.ProductVariationRequest;
import com.tien.aivirabackend.domain.dto.response.ProductResponse;
import com.tien.aivirabackend.domain.entity.catalog.Category;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.entity.catalog.ProductVariation;
import com.tien.aivirabackend.domain.mapper.ProductMapper;
import com.tien.aivirabackend.repository.CategoryRepository;
import com.tien.aivirabackend.repository.ProductMediaRepository;
import com.tien.aivirabackend.repository.ProductRepository;
import com.tien.aivirabackend.repository.ProductVariationRepository;
import com.tien.aivirabackend.service.media.CloudinaryStorageService;
import com.tien.aivirabackend.service.auth.CurrentUserService;
import com.tien.aivirabackend.service.media.FileValidatorService;
import com.tien.aivirabackend.service.catalog.ProductSpecifications;
import com.tien.aivirabackend.service.catalog.ProductStatusPolicy;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {
    @Mock
    ProductRepository productRepository;

    @Mock
    ProductVariationRepository variationRepository;

    @Mock
    ProductMediaRepository mediaRepository;

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    ProductMapper productMapper;

    @Mock
    FileValidatorService fileValidatorService;

    @Mock
    CloudinaryStorageService cloudinaryStorageService;

    @Mock
    CurrentUserService currentUserService;

    CloudinaryProperties cloudinaryProperties;

    @InjectMocks
    ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        cloudinaryProperties = new CloudinaryProperties();
        productService = new ProductServiceImpl(
                productRepository,
                variationRepository,
                mediaRepository,
                categoryRepository,
                productMapper,
                fileValidatorService,
                cloudinaryStorageService,
                cloudinaryProperties,
                currentUserService,
                new ProductSpecifications(),
                new ProductStatusPolicy());
    }

    @Test
    void createAdminProduct_shouldActivateImmediately() {
        Category category = buildCategory();
        ProductCreateRequest request = ProductCreateRequest.builder()
                .sku("PROD-1")
                .productName("Aivira Dress")
                .description("Nice dress")
                .categoryId(1L)
                .price(BigDecimal.valueOf(100))
                .variations(List.of(variationRequest("VAR-1", 3), variationRequest("VAR-2", 5)))
                .build();
        ProductResponse response =
                ProductResponse.builder().id(1L).status(ProductStatus.ACTIVE).build();

        when(currentUserService.getCurrentUserId()).thenReturn("admin-1");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.existsBySku("PROD-1")).thenReturn(false);
        when(productRepository.existsBySlug("aivira-dress")).thenReturn(false);
        when(variationRepository.existsBySku("VAR-1")).thenReturn(false);
        when(variationRepository.existsBySku("VAR-2")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productMapper.toResponse(any(Product.class))).thenReturn(response);

        ProductResponse result = productService.createAdminProduct(request);

        assertThat(result).isSameAs(response);
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        Product savedProduct = productCaptor.getValue();
        assertThat(savedProduct.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(savedProduct.getActive()).isTrue();
        assertThat(savedProduct.getApprovedBy()).isEqualTo("admin-1");
        assertThat(savedProduct.getApprovedAt()).isNotNull();
        assertThat(savedProduct.getStockQuantity()).isEqualTo(8);
        assertThat(savedProduct.getProductVariations()).hasSize(2);
    }

    @Test
    void updateAdminProduct_shouldKeepActiveProductActive() {
        Product product = buildProduct(ProductStatus.ACTIVE);
        ProductUpdateRequest request = ProductUpdateRequest.builder()
                .productName("Updated Dress")
                .price(BigDecimal.valueOf(120))
                .build();
        ProductResponse response =
                ProductResponse.builder().id(1L).status(ProductStatus.ACTIVE).build();

        when(productRepository.findDetailedById(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsBySlugAndIdNot("updated-dress", 1L)).thenReturn(false);
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.updateAdminProduct(1L, request);

        assertThat(result).isSameAs(response);
        assertThat(product.getProductName()).isEqualTo("Updated Dress");
        assertThat(product.getPrice()).isEqualByComparingTo("120");
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(product.getActive()).isTrue();
    }

    @Test
    void deleteAdminProduct_shouldSoftDeleteProduct() {
        Product product = buildProduct(ProductStatus.ACTIVE);
        when(productRepository.findDetailedById(1L)).thenReturn(Optional.of(product));

        productService.deleteAdminProduct(1L);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.INACTIVE);
        assertThat(product.getActive()).isFalse();
        verify(productRepository).save(product);
    }

    @Test
    void updateVariationStock_shouldNotMoveProductToDraft() {
        Product product = buildProduct(ProductStatus.ACTIVE);
        ProductVariation variation = activeVariation(product, "VAR-1", 4);
        product.getProductVariations().add(variation);

        when(productRepository.findDetailedById(1L)).thenReturn(Optional.of(product));
        when(variationRepository.findByIdAndProductId(2L, 1L)).thenReturn(Optional.of(variation));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product))
                .thenReturn(ProductResponse.builder().status(ProductStatus.ACTIVE).build());

        productService.updateVariationStock(
                1L,
                2L,
                com.tien.aivirabackend.domain.dto.request.StockUpdateRequest.builder()
                        .stockQuantity(9)
                        .build());

        assertThat(variation.getStockQuantity()).isEqualTo(9);
        assertThat(product.getStockQuantity()).isEqualTo(9);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
    }

    private ProductVariationRequest variationRequest(String sku, int stock) {
        return ProductVariationRequest.builder()
                .sku(sku)
                .color("Black")
                .size("M")
                .additionalPrice(BigDecimal.ZERO)
                .stockQuantity(stock)
                .build();
    }

    private Category buildCategory() {
        return Category.builder()
                .id(1L)
                .categoryName("Fashion")
                .slug("fashion")
                .description("Fashion")
                .active(true)
                .visible(true)
                .build();
    }

    private Product buildProduct(ProductStatus status) {
        return Product.builder()
                .id(1L)
                .category(buildCategory())
                .sku("PROD-1")
                .productName("Aivira Dress")
                .slug("aivira-dress")
                .description("Nice dress")
                .price(BigDecimal.valueOf(100))
                .stockQuantity(0)
                .soldCount(0)
                .active(status != ProductStatus.INACTIVE)
                .featured(false)
                .status(status)
                .build();
    }

    private ProductVariation activeVariation(Product product, String sku, int stock) {
        return ProductVariation.builder()
                .id(2L)
                .product(product)
                .sku(sku)
                .color("Black")
                .size("M")
                .additionalPrice(BigDecimal.ZERO)
                .stockQuantity(stock)
                .active(true)
                .build();
    }

}
