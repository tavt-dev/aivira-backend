package com.tien.aivirabackend.controller;

import java.math.BigDecimal;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.tien.aivirabackend.constant.ProductStatus;
import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.ProductCreateRequest;
import com.tien.aivirabackend.domain.dto.request.ProductMediaUpdateRequest;
import com.tien.aivirabackend.domain.dto.request.ProductUpdateRequest;
import com.tien.aivirabackend.domain.dto.request.ProductVariationRequest;
import com.tien.aivirabackend.domain.dto.request.ShopModerationRequest;
import com.tien.aivirabackend.domain.dto.request.StockUpdateRequest;
import com.tien.aivirabackend.domain.dto.response.ProductResponse;
import com.tien.aivirabackend.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductController {
    ProductService productService;

    @GetMapping("/products")
    @Tag(name = "Public Catalog")
    @Operation(summary = "Search public products")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getPublicProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) String shopSlug,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                "Get products successful",
                productService.getPublicProducts(
                        keyword, categorySlug, shopSlug, brand, minPrice, maxPrice, available, sort, page, size)));
    }

    @GetMapping("/products/{slug}")
    @Tag(name = "Public Catalog")
    @Operation(summary = "Get public product detail")
    public ResponseEntity<ApiResponse<ProductResponse>> getPublicProduct(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success("Get product successful", productService.getPublicProduct(slug)));
    }

    @GetMapping("/seller/products")
    @Tag(name = "Seller Products")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List current seller products")
    @PreAuthorize("@authorizationService.hasAnyPermission('PRODUCT_CREATE_OWN_SHOP', 'PRODUCT_UPDATE_OWN_SHOP')")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getSellerProducts(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                "Get seller products successful", productService.getSellerProducts(status, keyword, page, size)));
    }

    @GetMapping("/seller/products/{productId}")
    @Tag(name = "Seller Products")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get current seller product")
    @PreAuthorize("@authorizationService.hasAnyPermission('PRODUCT_CREATE_OWN_SHOP', 'PRODUCT_UPDATE_OWN_SHOP')")
    public ResponseEntity<ApiResponse<ProductResponse>> getSellerProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(
                ApiResponse.success("Get seller product successful", productService.getSellerProduct(productId)));
    }

    @PostMapping("/seller/products")
    @Tag(name = "Seller Products")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create seller product")
    @PreAuthorize("@authorizationService.hasPermission('PRODUCT_CREATE_OWN_SHOP')")
    public ResponseEntity<ApiResponse<ProductResponse>> createSellerProduct(
            @Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Create product successful", productService.createSellerProduct(request)));
    }

    @PutMapping("/seller/products/{productId}")
    @Tag(name = "Seller Products")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update seller product")
    @PreAuthorize("@authorizationService.hasPermission('PRODUCT_UPDATE_OWN_SHOP')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateSellerProduct(
            @PathVariable Long productId, @Valid @RequestBody ProductUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Update product successful", productService.updateSellerProduct(productId, request)));
    }

    @DeleteMapping("/seller/products/{productId}")
    @Tag(name = "Seller Products")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Soft delete seller product")
    @PreAuthorize("@authorizationService.hasPermission('PRODUCT_DELETE_OWN_SHOP')")
    public ResponseEntity<ApiResponse<Void>> deleteSellerProduct(@PathVariable Long productId) {
        productService.deleteSellerProduct(productId);
        return ResponseEntity.ok(ApiResponse.success("Delete product successful", null));
    }

    @PostMapping("/seller/products/{productId}/submit-review")
    @Tag(name = "Seller Products")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Submit seller product for review")
    @PreAuthorize("@authorizationService.hasPermission('PRODUCT_SUBMIT_REVIEW_OWN_SHOP')")
    public ResponseEntity<ApiResponse<ProductResponse>> submitSellerProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(
                ApiResponse.success("Submit product successful", productService.submitSellerProduct(productId)));
    }

    @PostMapping(value = "/seller/products/{productId}/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Tag(name = "Seller Products")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Upload product media")
    @PreAuthorize("@authorizationService.hasPermission('PRODUCT_MEDIA_UPLOAD_OWN_SHOP')")
    public ResponseEntity<ApiResponse<ProductResponse>> uploadProductMedia(
            @PathVariable Long productId,
            @Parameter(description = "Product image file") @RequestParam("media") MultipartFile mediaFile,
            @RequestParam(required = false) String altText,
            @RequestParam(required = false) Integer sortOrder,
            @RequestParam(required = false) Boolean primary) {
        return ResponseEntity.ok(ApiResponse.success(
                "Upload product media successful",
                productService.uploadProductMedia(productId, mediaFile, altText, sortOrder, primary)));
    }

    @PutMapping("/seller/products/{productId}/media/{mediaId}")
    @Tag(name = "Seller Products")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update product media")
    @PreAuthorize("@authorizationService.hasPermission('PRODUCT_MEDIA_UPDATE_OWN_SHOP')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProductMedia(
            @PathVariable Long productId,
            @PathVariable Long mediaId,
            @Valid @RequestBody ProductMediaUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Update product media successful", productService.updateProductMedia(productId, mediaId, request)));
    }

    @DeleteMapping("/seller/products/{productId}/media/{mediaId}")
    @Tag(name = "Seller Products")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Soft delete product media")
    @PreAuthorize("@authorizationService.hasPermission('PRODUCT_MEDIA_DELETE_OWN_SHOP')")
    public ResponseEntity<ApiResponse<Void>> deleteProductMedia(
            @PathVariable Long productId, @PathVariable Long mediaId) {
        productService.deleteProductMedia(productId, mediaId);
        return ResponseEntity.ok(ApiResponse.success("Delete product media successful", null));
    }

    @PostMapping("/seller/products/{productId}/variations")
    @Tag(name = "Seller Products")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create product variation")
    @PreAuthorize("@authorizationService.hasPermission('PRODUCT_UPDATE_OWN_SHOP')")
    public ResponseEntity<ApiResponse<ProductResponse>> createVariation(
            @PathVariable Long productId, @Valid @RequestBody ProductVariationRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Create variation successful", productService.createVariation(productId, request)));
    }

    @PutMapping("/seller/products/{productId}/variations/{variationId}")
    @Tag(name = "Seller Products")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update product variation")
    @PreAuthorize("@authorizationService.hasPermission('PRODUCT_UPDATE_OWN_SHOP')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateVariation(
            @PathVariable Long productId,
            @PathVariable Long variationId,
            @Valid @RequestBody ProductVariationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Update variation successful", productService.updateVariation(productId, variationId, request)));
    }

    @DeleteMapping("/seller/products/{productId}/variations/{variationId}")
    @Tag(name = "Seller Products")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Soft delete product variation")
    @PreAuthorize("@authorizationService.hasPermission('PRODUCT_UPDATE_OWN_SHOP')")
    public ResponseEntity<ApiResponse<Void>> deleteVariation(
            @PathVariable Long productId, @PathVariable Long variationId) {
        productService.deleteVariation(productId, variationId);
        return ResponseEntity.ok(ApiResponse.success("Delete variation successful", null));
    }

    @PutMapping("/seller/products/{productId}/variations/{variationId}/stock")
    @Tag(name = "Seller Products")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update variation stock")
    @PreAuthorize("@authorizationService.hasPermission('INVENTORY_UPDATE_OWN_SHOP')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateVariationStock(
            @PathVariable Long productId,
            @PathVariable Long variationId,
            @Valid @RequestBody StockUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Update stock successful", productService.updateVariationStock(productId, variationId, request)));
    }

    @GetMapping("/admin/products")
    @Tag(name = "Admin Products")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List products for admin")
    @PreAuthorize("@authorizationService.hasAnyPermission('PRODUCT_MANAGE_ALL', 'PRODUCT_APPROVE', 'PRODUCT_REJECT')")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getAdminProducts(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                "Get admin products successful",
                productService.getAdminProducts(status, shopId, categoryId, keyword, page, size)));
    }

    @GetMapping("/admin/products/{productId}")
    @Tag(name = "Admin Products")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get admin product detail")
    @PreAuthorize("@authorizationService.hasAnyPermission('PRODUCT_MANAGE_ALL', 'PRODUCT_APPROVE', 'PRODUCT_REJECT')")
    public ResponseEntity<ApiResponse<ProductResponse>> getAdminProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(
                ApiResponse.success("Get admin product successful", productService.getAdminProduct(productId)));
    }

    @PutMapping("/admin/products/{productId}/approve")
    @Tag(name = "Admin Products")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Approve product")
    @PreAuthorize("@authorizationService.hasAnyPermission('PRODUCT_MANAGE_ALL', 'PRODUCT_APPROVE')")
    public ResponseEntity<ApiResponse<ProductResponse>> approve(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success("Approve product successful", productService.approve(productId)));
    }

    @PutMapping("/admin/products/{productId}/reject")
    @Tag(name = "Admin Products")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Reject product")
    @PreAuthorize("@authorizationService.hasAnyPermission('PRODUCT_MANAGE_ALL', 'PRODUCT_REJECT')")
    public ResponseEntity<ApiResponse<ProductResponse>> reject(
            @PathVariable Long productId, @Valid @RequestBody ShopModerationRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Reject product successful", productService.reject(productId, request)));
    }
}
