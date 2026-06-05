package com.tien.aivirabackend.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.PromotionCreateRequest;
import com.tien.aivirabackend.domain.dto.request.PromotionUpdateRequest;
import com.tien.aivirabackend.domain.dto.response.PromotionResponse;
import com.tien.aivirabackend.service.discount.PromotionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/admin/promotions")
@Tag(name = "Admin Promotions")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminPromotionController {
    PromotionService promotionService;

    @GetMapping
    @Operation(summary = "List promotions")
    @PreAuthorize("@authorizationService.hasAnyPermission('PROMOTION_MANAGE_ALL', 'PROMOTION_READ')")
    public ResponseEntity<ApiResponse<PageResponse<PromotionResponse>>> getPromotions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                ApiResponse.success("Get promotions successful", promotionService.getPromotions(page, size)));
    }

    @GetMapping("/{promotionId}")
    @Operation(summary = "Get promotion detail")
    @PreAuthorize("@authorizationService.hasAnyPermission('PROMOTION_MANAGE_ALL', 'PROMOTION_READ')")
    public ResponseEntity<ApiResponse<PromotionResponse>> getPromotion(@PathVariable Long promotionId) {
        return ResponseEntity.ok(
                ApiResponse.success("Get promotion successful", promotionService.getPromotion(promotionId)));
    }

    @PostMapping
    @Operation(
            summary = "Create promotion",
            description = "Creates a product-scope or category-scope promotion. Promotions apply before order coupons.")
    @PreAuthorize("@authorizationService.hasAnyPermission('PROMOTION_MANAGE_ALL', 'PROMOTION_CREATE_ALL')")
    public ResponseEntity<ApiResponse<PromotionResponse>> createPromotion(
            @Valid @RequestBody PromotionCreateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Create promotion successful", promotionService.createPromotion(request)));
    }

    @PutMapping("/{promotionId}")
    @Operation(summary = "Update promotion", description = "Updates an active or inactive promotion rule.")
    @PreAuthorize("@authorizationService.hasAnyPermission('PROMOTION_MANAGE_ALL', 'PROMOTION_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<PromotionResponse>> updatePromotion(
            @PathVariable Long promotionId, @Valid @RequestBody PromotionUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Update promotion successful", promotionService.updatePromotion(promotionId, request)));
    }

    @DeleteMapping("/{promotionId}")
    @Operation(summary = "Deactivate promotion", description = "Soft-deactivates a promotion instead of deleting historical order snapshots.")
    @PreAuthorize("@authorizationService.hasAnyPermission('PROMOTION_MANAGE_ALL', 'PROMOTION_DELETE_ALL')")
    public ResponseEntity<ApiResponse<Void>> deletePromotion(@PathVariable Long promotionId) {
        promotionService.deletePromotion(promotionId);
        return ResponseEntity.ok(ApiResponse.success("Delete promotion successful", null));
    }
}
