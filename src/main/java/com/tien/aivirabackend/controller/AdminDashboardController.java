package com.tien.aivirabackend.controller;

import java.time.Instant;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.domain.dto.response.DashboardLowStockResponse;
import com.tien.aivirabackend.domain.dto.response.DashboardOrdersResponse;
import com.tien.aivirabackend.domain.dto.response.DashboardSalesResponse;
import com.tien.aivirabackend.domain.dto.response.DashboardSummaryResponse;
import com.tien.aivirabackend.domain.dto.response.DashboardTopBooksResponse;
import com.tien.aivirabackend.service.dashboard.DashboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/admin/dashboard")
@Tag(name = "Admin Dashboard")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminDashboardController {
    DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Get admin dashboard summary", description = "Returns revenue, order, payment, user, pending, and low-stock summary metrics.")
    @PreAuthorize("@authorizationService.hasAnyPermission('DASHBOARD_READ_ADMIN', 'REPORT_READ_ALL')")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate) {
        return ResponseEntity.ok(
                ApiResponse.success("Get dashboard summary successful", dashboardService.getSummary(fromDate, toDate)));
    }

    @GetMapping("/sales")
    @Operation(summary = "Get admin dashboard sales", description = "Returns daily revenue and order-count points for a date range.")
    @PreAuthorize("@authorizationService.hasAnyPermission('DASHBOARD_READ_ADMIN', 'REPORT_READ_ALL')")
    public ResponseEntity<ApiResponse<DashboardSalesResponse>> getSales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate) {
        return ResponseEntity
                .ok(ApiResponse.success("Get dashboard sales successful", dashboardService.getSales(fromDate, toDate)));
    }

    @GetMapping("/orders")
    @Operation(summary = "Get admin dashboard orders", description = "Returns order counts grouped by order status.")
    @PreAuthorize("@authorizationService.hasAnyPermission('DASHBOARD_READ_ADMIN', 'REPORT_READ_ALL')")
    public ResponseEntity<ApiResponse<DashboardOrdersResponse>> getOrders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate) {
        return ResponseEntity.ok(
                ApiResponse.success("Get dashboard orders successful", dashboardService.getOrders(fromDate, toDate)));
    }

    @GetMapping("/top-books")
    @Operation(summary = "Get admin dashboard top books", description = "Returns top-selling books from order item aggregates with soldCount fallback.")
    @PreAuthorize("@authorizationService.hasAnyPermission('DASHBOARD_READ_ADMIN', 'REPORT_READ_ALL')")
    public ResponseEntity<ApiResponse<DashboardTopBooksResponse>> getTopBooks(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success("Get dashboard top books successful",
                dashboardService.getTopBooks(fromDate, toDate, limit)));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get admin dashboard low stock books", description = "Returns active books whose stock is less than or equal to the threshold.")
    @PreAuthorize("@authorizationService.hasAnyPermission('DASHBOARD_READ_ADMIN', 'REPORT_READ_ALL')")
    public ResponseEntity<ApiResponse<DashboardLowStockResponse>> getLowStock(
            @RequestParam(required = false) Integer threshold, @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success("Get dashboard low stock successful",
                dashboardService.getLowStock(threshold, limit)));
    }
}
