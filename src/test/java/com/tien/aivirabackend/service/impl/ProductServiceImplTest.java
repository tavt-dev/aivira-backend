package com.tien.aivirabackend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.tien.aivirabackend.constant.ShopStatus;
import com.tien.aivirabackend.domain.dto.request.ProductCreateRequest;
import com.tien.aivirabackend.domain.dto.request.ProductVariationRequest;
import com.tien.aivirabackend.domain.dto.response.ProductResponse;
import com.tien.aivirabackend.domain.entity.catalog.Category;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.entity.catalog.ProductVariation;
import com.tien.aivirabackend.domain.entity.marketplace.Shop;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.mapper.ProductMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.repository.CategoryRepository;
import com.tien.aivirabackend.repository.ProductMediaRepository;
import com.tien.aivirabackend.repository.ProductRepository;
import com.tien.aivirabackend.repository.ProductVariationRepository;
import com.tien.aivirabackend.service.CloudinaryStorageService;
import com.tien.aivirabackend.service.FileValidatorService;
import com.tien.aivirabackend.service.ShopOwnershipService;

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
    ShopOwnershipService shopOwnershipService;

    @Mock
    ProductMapper productMapper;

    @Mock
    FileValidatorService fileValidatorService;

    @Mock
    CloudinaryStorageService cloudinaryStorageService;

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
                shopOwnershipService,
                productMapper,
                fileValidatorService,
                cloudinaryStorageService,
                cloudinaryProperties);
    }

    @Test
    void createSellerProduct_shouldAttachApprovedShopAndRecalculateStockFromVariations() {
        Shop shop = buildShop(ShopStatus.APPROVED);
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
                ProductResponse.builder().id(1L).status(ProductStatus.DRAFT).build();

        when(shopOwnershipService.requireCurrentUserApprovedShop()).thenReturn(shop);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.existsBySku("PROD-1")).thenReturn(false);
        when(productRepository.existsBySlug("aivira-dress")).thenReturn(false);
        when(variationRepository.existsBySku("VAR-1")).thenReturn(false);
        when(variationRepository.existsBySku("VAR-2")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productMapper.toResponse(any(Product.class))).thenReturn(response);

        ProductResponse result = productService.createSellerProduct(request);

        assertThat(result).isSameAs(response);
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        Product savedProduct = productCaptor.getValue();
        assertThat(savedProduct.getShop()).isSameAs(shop);
        assertThat(savedProduct.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(savedProduct.getStockQuantity()).isEqualTo(8);
        assertThat(savedProduct.getProductVariations()).hasSize(2);
    }

    @Test
    void submitSellerProduct_shouldMoveDraftToPendingReviewWhenVariationExists() {
        Shop shop = buildShop(ShopStatus.APPROVED);
        Product product = buildProduct(shop, ProductStatus.DRAFT);
        ProductVariation variation = ProductVariation.builder()
                .id(1L)
                .sku("VAR-1")
                .color("Black")
                .size("M")
                .additionalPrice(BigDecimal.ZERO)
                .stockQuantity(4)
                .active(true)
                .product(product)
                .build();
        product.getProductVariations().add(variation);
        ProductResponse response =
                ProductResponse.builder().status(ProductStatus.PENDING_REVIEW).build();

        when(shopOwnershipService.requireCurrentUserApprovedShop()).thenReturn(shop);
        when(productRepository.findDetailedById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.submitSellerProduct(1L);

        assertThat(result).isSameAs(response);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.PENDING_REVIEW);
        assertThat(product.getSubmittedAt()).isNotNull();
    }

    @Test
    void submitSellerProduct_shouldRejectProductWithoutActiveVariation() {
        Shop shop = buildShop(ShopStatus.APPROVED);
        Product product = buildProduct(shop, ProductStatus.DRAFT);

        when(shopOwnershipService.requireCurrentUserApprovedShop()).thenReturn(shop);
        when(productRepository.findDetailedById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.submitSellerProduct(1L)).isInstanceOf(AppException.class);
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

    private Product buildProduct(Shop shop, ProductStatus status) {
        return Product.builder()
                .id(1L)
                .shop(shop)
                .category(buildCategory())
                .sku("PROD-1")
                .productName("Aivira Dress")
                .slug("aivira-dress")
                .description("Nice dress")
                .price(BigDecimal.valueOf(100))
                .stockQuantity(0)
                .soldCount(0)
                .active(true)
                .featured(false)
                .status(status)
                .build();
    }

    private Shop buildShop(ShopStatus status) {
        User owner = new User();
        owner.setId("seller-1");
        return Shop.builder()
                .id(1L)
                .owner(owner)
                .shopName("Aivira Fashion")
                .slug("aivira-fashion")
                .businessEmail("shop@example.com")
                .phoneNumber("0900000000")
                .legalName("Aivira Fashion LLC")
                .pickupAddressLine("123 Street")
                .pickupCity("Ho Chi Minh")
                .status(status)
                .build();
    }
}
