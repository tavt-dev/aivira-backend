package com.tien.aivirabackend.controller;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.domain.dto.request.ApplyShopRequest;
import com.tien.aivirabackend.domain.dto.request.UpdateShopRequest;
import com.tien.aivirabackend.domain.dto.response.SellerDashboardResponse;
import com.tien.aivirabackend.domain.dto.response.ShopResponse;
import com.tien.aivirabackend.service.ShopService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "SELLER-SHOP-CONTROLLER")
@RestController
@RequestMapping("/seller")
@Tag(name = "Seller Shop", description = "Seller onboarding and shop profile APIs")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SellerShopController {
    ShopService shopService;

    @PostMapping("/shop/apply")
    @Operation(summary = "Apply for seller shop", description = "Creates a pending shop application for current user.")
    @PreAuthorize("@authorizationService.hasPermission('SELLER_APPLY')")
    public ResponseEntity<ApiResponse<ShopResponse>> apply(@Valid @RequestBody ApplyShopRequest request) {
        log.info("Apply seller shop request");
        return ResponseEntity.ok(ApiResponse.success("Apply shop successful", shopService.apply(request)));
    }

    @GetMapping("/shop")
    @Operation(summary = "Get my shop", description = "Returns the current user's shop profile.")
    @PreAuthorize("@authorizationService.hasAnyPermission('SELLER_APPLY', 'SHOP_READ_SELF')")
    public ResponseEntity<ApiResponse<ShopResponse>> getMyShop() {
        log.info("Get my shop request");
        return ResponseEntity.ok(ApiResponse.success("Get shop successful", shopService.getMyShop()));
    }

    @PutMapping("/shop")
    @Operation(summary = "Update my shop", description = "Updates the current user's shop profile.")
    @PreAuthorize("@authorizationService.hasAnyPermission('SELLER_APPLY', 'SHOP_UPDATE_SELF')")
    public ResponseEntity<ApiResponse<ShopResponse>> updateMyShop(@Valid @RequestBody UpdateShopRequest request) {
        log.info("Update my shop request");
        return ResponseEntity.ok(ApiResponse.success("Update shop successful", shopService.updateMyShop(request)));
    }

    @PostMapping("/shop/resubmit")
    @Operation(summary = "Resubmit rejected shop", description = "Moves a rejected shop application back to pending.")
    @PreAuthorize("@authorizationService.hasPermission('SELLER_APPLY')")
    public ResponseEntity<ApiResponse<ShopResponse>> resubmitMyShop() {
        log.info("Resubmit my shop request");
        return ResponseEntity.ok(ApiResponse.success("Resubmit shop successful", shopService.resubmitMyShop()));
    }

    @PutMapping(value = "/shop/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update shop logo", description = "Uploads a logo image for current user's shop.")
    @PreAuthorize("@authorizationService.hasAnyPermission('SELLER_APPLY', 'SHOP_UPDATE_SELF')")
    public ResponseEntity<ApiResponse<ShopResponse>> updateMyShopLogo(
            @Parameter(description = "Shop logo image file") @RequestParam("logo") MultipartFile logoFile) {
        log.info("Update my shop logo request");
        return ResponseEntity.ok(
                ApiResponse.success("Update shop logo successful", shopService.updateMyShopLogo(logoFile)));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get seller dashboard", description = "Returns Phase 3 seller dashboard placeholder metrics.")
    @PreAuthorize("@authorizationService.hasPermission('DASHBOARD_READ_SELLER')")
    public ResponseEntity<ApiResponse<SellerDashboardResponse>> getDashboard() {
        log.info("Get seller dashboard request");
        return ResponseEntity.ok(ApiResponse.success("Get seller dashboard successful", shopService.getDashboard()));
    }
}
