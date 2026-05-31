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
import com.tien.aivirabackend.domain.dto.request.StockUpdateRequest;
import com.tien.aivirabackend.domain.dto.response.ProductResponse;
import com.tien.aivirabackend.service.catalog.ProductService;

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
                        keyword, categorySlug, brand, minPrice, maxPrice, available, sort, page, size)));
    }

    @GetMapping("/products/{slug}")
    @Tag(name = "Public Catalog")
    @Operation(summary = "Get public product detail")
    public ResponseEntity<ApiResponse<ProductResponse>> getPublicProduct(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success("Get product successful", productService.getPublicProduct(slug)));
    }

    @GetMapping("/admin/products")
    @Tag(name = "Admin Products")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List products for admin")
    @PreAuthorize("@authorizationService.hasAnyPermission('PRODUCT_MANAGE_ALL', 'PRODUCT_READ')")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getAdminProducts(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                "Get admin products successful",
                productService.getAdminProducts(status, categoryId, keyword, page, size)));
    }

    @GetMapping("/admin/products/{productId}")
    @Tag(name = "Admin Products")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get admin product detail")
    @PreAuthorize("@authorizationService.hasAnyPermission('PRODUCT_MANAGE_ALL', 'PRODUCT_READ')")
    public ResponseEntity<ApiResponse<ProductResponse>> getAdminProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(
                ApiResponse.success("Get admin product successful", productService.getAdminProduct(productId)));
    }

    @PostMapping("/admin/products")
    @Tag(name = "Admin Products")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create admin product")
    @PreAuthorize("@authorizationService.hasPermission('PRODUCT_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<ProductResponse>> createAdminProduct(
            @Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Create product successful", productService.createAdminProduct(request)));
    }

    @PutMapping("/admin/products/{productId}")
    @Tag(name = "Admin Products")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update admin product")
    @PreAuthorize("@authorizationService.hasPermission('PRODUCT_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateAdminProduct(
            @PathVariable Long productId, @Valid @RequestBody ProductUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Update product successful", productService.updateAdminProduct(productId, request)));
    }

    @DeleteMapping("/admin/products/{productId}")
    @Tag(name = "Admin Products")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Soft delete admin product")
    @PreAuthorize("@authorizationService.hasPermission('PRODUCT_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<Void>> deleteAdminProduct(@PathVariable Long productId) {
        productService.deleteAdminProduct(productId);
        return ResponseEntity.ok(ApiResponse.success("Delete product successful", null));
    }

    @PostMapping(value = "/admin/products/{productId}/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Tag(name = "Admin Products")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Upload product media")
    @PreAuthorize("@authorizationService.hasAnyPermission('PRODUCT_MANAGE_ALL', 'PRODUCT_MEDIA_MANAGE_ALL')")
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

    @PutMapping("/admin/products/{productId}/media/{mediaId}")
    @Tag(name = "Admin Products")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update product media")
    @PreAuthorize("@authorizationService.hasAnyPermission('PRODUCT_MANAGE_ALL', 'PRODUCT_MEDIA_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProductMedia(
            @PathVariable Long productId,
            @PathVariable Long mediaId,
            @Valid @RequestBody ProductMediaUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Update product media successful", productService.updateProductMedia(productId, mediaId, request)));
    }

    @DeleteMapping("/admin/products/{productId}/media/{mediaId}")
    @Tag(name = "Admin Products")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Soft delete product media")
    @PreAuthorize("@authorizationService.hasAnyPermission('PRODUCT_MANAGE_ALL', 'PRODUCT_MEDIA_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<Void>> deleteProductMedia(
            @PathVariable Long productId, @PathVariable Long mediaId) {
        productService.deleteProductMedia(productId, mediaId);
        return ResponseEntity.ok(ApiResponse.success("Delete product media successful", null));
    }

    @PostMapping("/admin/products/{productId}/variations")
    @Tag(name = "Admin Products")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create product variation")
    @PreAuthorize("@authorizationService.hasPermission('PRODUCT_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<ProductResponse>> createVariation(
            @PathVariable Long productId, @Valid @RequestBody ProductVariationRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Create variation successful", productService.createVariation(productId, request)));
    }

    @PutMapping("/admin/products/{productId}/variations/{variationId}")
    @Tag(name = "Admin Products")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update product variation")
    @PreAuthorize("@authorizationService.hasPermission('PRODUCT_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateVariation(
            @PathVariable Long productId,
            @PathVariable Long variationId,
            @Valid @RequestBody ProductVariationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Update variation successful", productService.updateVariation(productId, variationId, request)));
    }

    @DeleteMapping("/admin/products/{productId}/variations/{variationId}")
    @Tag(name = "Admin Products")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Soft delete product variation")
    @PreAuthorize("@authorizationService.hasPermission('PRODUCT_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<Void>> deleteVariation(
            @PathVariable Long productId, @PathVariable Long variationId) {
        productService.deleteVariation(productId, variationId);
        return ResponseEntity.ok(ApiResponse.success("Delete variation successful", null));
    }

    @PutMapping("/admin/products/{productId}/variations/{variationId}/stock")
    @Tag(name = "Admin Products")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update variation stock")
    @PreAuthorize("@authorizationService.hasAnyPermission('PRODUCT_MANAGE_ALL', 'INVENTORY_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateVariationStock(
            @PathVariable Long productId,
            @PathVariable Long variationId,
            @Valid @RequestBody StockUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Update stock successful", productService.updateVariationStock(productId, variationId, request)));
    }
}
