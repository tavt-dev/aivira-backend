package com.tien.aivirabackend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.domain.dto.response.StorefrontHomeResponse;
import com.tien.aivirabackend.service.storefront.StorefrontService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@Tag(name = "Storefront")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StorefrontController {
    StorefrontService storefrontService;

    @GetMapping("/storefront/home")
    @Operation(summary = "Get storefront home data", description = "Returns featured books, new arrivals, bestselling books, and category highlights for the bookstore homepage.")
    public ResponseEntity<ApiResponse<StorefrontHomeResponse>> getHome() {
        return ResponseEntity.ok(ApiResponse.success("Get storefront home successful", storefrontService.getHome()));
    }
}
