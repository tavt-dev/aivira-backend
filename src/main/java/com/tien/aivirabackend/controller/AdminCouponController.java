package com.tien.aivirabackend.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.CouponCreateRequest;
import com.tien.aivirabackend.domain.dto.request.CouponUpdateRequest;
import com.tien.aivirabackend.domain.dto.response.CouponResponse;
import com.tien.aivirabackend.service.discount.CouponService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/admin/coupons")
@Tag(name = "Admin Coupons")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminCouponController {
    CouponService couponService;

    @GetMapping
    @Operation(summary = "List coupons")
    @PreAuthorize("@authorizationService.hasPermission('COUPON_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<PageResponse<CouponResponse>>> getCoupons(
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("Get coupons successful", couponService.getCoupons(page, size)));
    }

    @GetMapping("/{couponId}")
    @Operation(summary = "Get coupon detail")
    @PreAuthorize("@authorizationService.hasPermission('COUPON_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<CouponResponse>> getCoupon(@PathVariable Long couponId) {
        return ResponseEntity.ok(ApiResponse.success("Get coupon successful", couponService.getCoupon(couponId)));
    }

    @PostMapping
    @Operation(summary = "Create coupon", description = "Creates an order-level coupon. Coupon codes are normalized to uppercase and applied after promotions.")
    @PreAuthorize("@authorizationService.hasAnyPermission('COUPON_MANAGE_ALL', 'COUPON_CREATE_ALL')")
    public ResponseEntity<ApiResponse<CouponResponse>> createCoupon(@Valid @RequestBody CouponCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Create coupon successful", couponService.createCoupon(request)));
    }

    @PutMapping("/{couponId}")
    @Operation(summary = "Update coupon", description = "Updates coupon rules while preserving usage history.")
    @PreAuthorize("@authorizationService.hasAnyPermission('COUPON_MANAGE_ALL', 'COUPON_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<CouponResponse>> updateCoupon(@PathVariable Long couponId,
            @Valid @RequestBody CouponUpdateRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("Update coupon successful", couponService.updateCoupon(couponId, request)));
    }

    @DeleteMapping("/{couponId}")
    @Operation(summary = "Deactivate coupon", description = "Soft-deactivates a coupon instead of deleting historical usage.")
    @PreAuthorize("@authorizationService.hasAnyPermission('COUPON_MANAGE_ALL', 'COUPON_DELETE_ALL')")
    public ResponseEntity<ApiResponse<Void>> deleteCoupon(@PathVariable Long couponId) {
        couponService.deleteCoupon(couponId);
        return ResponseEntity.ok(ApiResponse.success("Delete coupon successful", null));
    }
}
