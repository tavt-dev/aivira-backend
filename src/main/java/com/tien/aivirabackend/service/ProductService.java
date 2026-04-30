package com.tien.aivirabackend.service;

import java.math.BigDecimal;

import org.springframework.web.multipart.MultipartFile;

import com.tien.aivirabackend.constant.ProductStatus;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.ProductCreateRequest;
import com.tien.aivirabackend.domain.dto.request.ProductMediaUpdateRequest;
import com.tien.aivirabackend.domain.dto.request.ProductUpdateRequest;
import com.tien.aivirabackend.domain.dto.request.ProductVariationRequest;
import com.tien.aivirabackend.domain.dto.request.ShopModerationRequest;
import com.tien.aivirabackend.domain.dto.request.StockUpdateRequest;
import com.tien.aivirabackend.domain.dto.response.ProductResponse;

public interface ProductService {
    PageResponse<ProductResponse> getPublicProducts(
            String keyword,
            String categorySlug,
            String shopSlug,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean available,
            String sort,
            int page,
            int size);

    ProductResponse getPublicProduct(String slug);

    PageResponse<ProductResponse> getSellerProducts(ProductStatus status, String keyword, int page, int size);

    ProductResponse getSellerProduct(Long productId);

    ProductResponse createSellerProduct(ProductCreateRequest request);

    ProductResponse updateSellerProduct(Long productId, ProductUpdateRequest request);

    void deleteSellerProduct(Long productId);

    ProductResponse submitSellerProduct(Long productId);

    ProductResponse uploadProductMedia(
            Long productId, MultipartFile mediaFile, String altText, Integer sortOrder, Boolean primary);

    ProductResponse updateProductMedia(Long productId, Long mediaId, ProductMediaUpdateRequest request);

    void deleteProductMedia(Long productId, Long mediaId);

    ProductResponse createVariation(Long productId, ProductVariationRequest request);

    ProductResponse updateVariation(Long productId, Long variationId, ProductVariationRequest request);

    void deleteVariation(Long productId, Long variationId);

    ProductResponse updateVariationStock(Long productId, Long variationId, StockUpdateRequest request);

    PageResponse<ProductResponse> getAdminProducts(
            ProductStatus status, Long shopId, Long categoryId, String keyword, int page, int size);

    ProductResponse getAdminProduct(Long productId);

    ProductResponse approve(Long productId);

    ProductResponse reject(Long productId, ShopModerationRequest request);
}
