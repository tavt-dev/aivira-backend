package com.tien.aivirabackend.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.tien.aivirabackend.constant.ShopStatus;
import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.ShopModerationRequest;
import com.tien.aivirabackend.domain.dto.response.ShopResponse;
import com.tien.aivirabackend.service.AdminShopService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "ADMIN-SHOP-CONTROLLER")
@RestController
@RequestMapping("/admin/shops")
@Tag(name = "Admin Shops", description = "Shop moderation and administration APIs")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminShopController {
    AdminShopService adminShopService;

    @GetMapping
    @Operation(summary = "List shops", description = "Lists shops with optional status and keyword filters.")
    @PreAuthorize("@authorizationService.hasAnyPermission('SHOP_MANAGE_ALL', 'SHOP_READ_ALL')")
    public ResponseEntity<ApiResponse<PageResponse<ShopResponse>>> getShops(
            @RequestParam(required = false) ShopStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("List shops request: status={} keyword={}", status, keyword);
        return ResponseEntity.ok(
                ApiResponse.success("Get shops successful", adminShopService.getShops(status, keyword, page, size)));
    }

    @GetMapping("/{shopId}")
    @Operation(summary = "Get shop detail", description = "Returns one shop by id.")
    @PreAuthorize("@authorizationService.hasAnyPermission('SHOP_MANAGE_ALL', 'SHOP_READ_ALL')")
    public ResponseEntity<ApiResponse<ShopResponse>> getShop(@PathVariable Long shopId) {
        log.info("Get shop request: shopId={}", shopId);
        return ResponseEntity.ok(ApiResponse.success("Get shop successful", adminShopService.getShop(shopId)));
    }

    @PutMapping("/{shopId}/approve")
    @Operation(summary = "Approve shop", description = "Approves a pending shop and assigns SELLER role to its owner.")
    @PreAuthorize("@authorizationService.hasAnyPermission('SHOP_MANAGE_ALL', 'SHOP_APPROVE')")
    public ResponseEntity<ApiResponse<ShopResponse>> approve(@PathVariable Long shopId) {
        log.info("Approve shop request: shopId={}", shopId);
        return ResponseEntity.ok(ApiResponse.success("Approve shop successful", adminShopService.approve(shopId)));
    }

    @PutMapping("/{shopId}/reject")
    @Operation(summary = "Reject shop", description = "Rejects a pending shop application.")
    @PreAuthorize("@authorizationService.hasAnyPermission('SHOP_MANAGE_ALL', 'SHOP_REJECT')")
    public ResponseEntity<ApiResponse<ShopResponse>> reject(
            @PathVariable Long shopId, @Valid @RequestBody ShopModerationRequest request) {
        log.info("Reject shop request: shopId={}", shopId);
        return ResponseEntity.ok(
                ApiResponse.success("Reject shop successful", adminShopService.reject(shopId, request)));
    }

    @PutMapping("/{shopId}/lock")
    @Operation(summary = "Lock shop", description = "Locks an approved shop.")
    @PreAuthorize("@authorizationService.hasAnyPermission('SHOP_MANAGE_ALL', 'SHOP_LOCK')")
    public ResponseEntity<ApiResponse<ShopResponse>> lock(
            @PathVariable Long shopId, @Valid @RequestBody ShopModerationRequest request) {
        log.info("Lock shop request: shopId={}", shopId);
        return ResponseEntity.ok(ApiResponse.success("Lock shop successful", adminShopService.lock(shopId, request)));
    }

    @PutMapping("/{shopId}/unlock")
    @Operation(summary = "Unlock shop", description = "Unlocks a locked shop.")
    @PreAuthorize("@authorizationService.hasAnyPermission('SHOP_MANAGE_ALL', 'SHOP_UNLOCK')")
    public ResponseEntity<ApiResponse<ShopResponse>> unlock(@PathVariable Long shopId) {
        log.info("Unlock shop request: shopId={}", shopId);
        return ResponseEntity.ok(ApiResponse.success("Unlock shop successful", adminShopService.unlock(shopId)));
    }
}
