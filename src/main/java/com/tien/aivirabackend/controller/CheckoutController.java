package com.tien.aivirabackend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.domain.dto.request.CheckoutRequest;
import com.tien.aivirabackend.domain.dto.response.CheckoutPreviewResponse;
import com.tien.aivirabackend.domain.dto.response.CheckoutResponse;
import com.tien.aivirabackend.service.auth.RequestMetadataService;
import com.tien.aivirabackend.service.commerce.CheckoutService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/checkout")
@Tag(name = "Checkout")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CheckoutController {
    CheckoutService checkoutService;
    RequestMetadataService requestMetadataService;

    @PostMapping("/preview")
    @Operation(summary = "Preview checkout totals")
    @PreAuthorize("@authorizationService.hasPermission('CHECKOUT_CREATE_SELF')")
    public ResponseEntity<ApiResponse<CheckoutPreviewResponse>> preview(@Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Checkout preview successful", checkoutService.preview(request)));
    }

    @PostMapping
    @Operation(summary = "Checkout selected cart items")
    @PreAuthorize("@authorizationService.hasPermission('CHECKOUT_CREATE_SELF')")
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(
            @Valid @RequestBody CheckoutRequest request, HttpServletRequest servletRequest) {
        return ResponseEntity.ok(ApiResponse.success(
                "Checkout successful", checkoutService.checkout(request, requestMetadataService.from(servletRequest))));
    }
}
