package com.tien.aivirabackend.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.domain.dto.request.CategoryRequest;
import com.tien.aivirabackend.domain.dto.response.CategoryResponse;
import com.tien.aivirabackend.service.CategoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryController {
    CategoryService categoryService;

    @GetMapping("/categories")
    @Tag(name = "Public Catalog")
    @Operation(summary = "List visible categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories() {
        return ResponseEntity.ok(
                ApiResponse.success("Get categories successful", categoryService.getVisibleCategories()));
    }

    @GetMapping("/categories/tree")
    @Tag(name = "Public Catalog")
    @Operation(summary = "Get visible category tree")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategoryTree() {
        return ResponseEntity.ok(
                ApiResponse.success("Get category tree successful", categoryService.getVisibleCategoryTree()));
    }

    @PostMapping("/admin/categories")
    @Tag(name = "Admin Categories")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create category")
    @PreAuthorize("@authorizationService.hasAnyPermission('CATEGORY_MANAGE_ALL', 'CATEGORY_CREATE')")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Create category successful", categoryService.create(request)));
    }

    @PutMapping("/admin/categories/{categoryId}")
    @Tag(name = "Admin Categories")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update category")
    @PreAuthorize("@authorizationService.hasAnyPermission('CATEGORY_MANAGE_ALL', 'CATEGORY_UPDATE')")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable Long categoryId, @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Update category successful", categoryService.update(categoryId, request)));
    }

    @DeleteMapping("/admin/categories/{categoryId}")
    @Tag(name = "Admin Categories")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Soft delete category")
    @PreAuthorize("@authorizationService.hasAnyPermission('CATEGORY_MANAGE_ALL', 'CATEGORY_DELETE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long categoryId) {
        categoryService.delete(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Delete category successful", null));
    }
}
