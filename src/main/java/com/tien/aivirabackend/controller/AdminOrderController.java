package com.tien.aivirabackend.controller;

import java.time.Instant;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.tien.aivirabackend.constant.OrderStatus;
import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.ManualRefundRequest;
import com.tien.aivirabackend.domain.dto.request.OrderCancelRequest;
import com.tien.aivirabackend.domain.dto.response.OrderResponse;
import com.tien.aivirabackend.domain.dto.response.OrderSummaryResponse;
import com.tien.aivirabackend.service.commerce.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/admin/orders")
@Tag(name = "Admin Orders")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminOrderController {
    OrderService orderService;

    @GetMapping
    @Operation(summary = "List orders for admin")
    @PreAuthorize("@authorizationService.hasAnyPermission('ORDER_MANAGE_ALL', 'ORDER_READ_ALL')")
    public ResponseEntity<ApiResponse<PageResponse<OrderSummaryResponse>>> getAdminOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                "Get admin orders successful",
                orderService.getAdminOrders(status, keyword, fromDate, toDate, page, size)));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get admin order detail")
    @PreAuthorize("@authorizationService.hasAnyPermission('ORDER_MANAGE_ALL', 'ORDER_READ_ALL')")
    public ResponseEntity<ApiResponse<OrderResponse>> getAdminOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(
                ApiResponse.success("Get admin order successful", orderService.getAdminOrder(orderId)));
    }

    @PutMapping("/{orderId}/confirm")
    @Operation(summary = "Confirm order")
    @PreAuthorize("@authorizationService.hasAnyPermission('ORDER_MANAGE_ALL', 'ORDER_UPDATE_STATUS_ALL')")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success("Confirm order successful", orderService.confirmOrder(orderId)));
    }

    @PutMapping("/{orderId}/packing")
    @Operation(summary = "Mark order as packing")
    @PreAuthorize("@authorizationService.hasAnyPermission('ORDER_MANAGE_ALL', 'ORDER_UPDATE_STATUS_ALL')")
    public ResponseEntity<ApiResponse<OrderResponse>> markPacking(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success("Mark order packing successful", orderService.markPacking(orderId)));
    }

    @PutMapping("/{orderId}/shipping")
    @Operation(summary = "Mark order as shipping")
    @PreAuthorize("@authorizationService.hasAnyPermission('ORDER_MANAGE_ALL', 'ORDER_UPDATE_STATUS_ALL')")
    public ResponseEntity<ApiResponse<OrderResponse>> markShipping(@PathVariable Long orderId) {
        return ResponseEntity.ok(
                ApiResponse.success("Mark order shipping successful", orderService.markShipping(orderId)));
    }

    @PutMapping("/{orderId}/completed")
    @Operation(summary = "Mark order as completed")
    @PreAuthorize("@authorizationService.hasAnyPermission('ORDER_MANAGE_ALL', 'ORDER_UPDATE_STATUS_ALL')")
    public ResponseEntity<ApiResponse<OrderResponse>> markCompleted(@PathVariable Long orderId) {
        return ResponseEntity.ok(
                ApiResponse.success("Mark order completed successful", orderService.markCompleted(orderId)));
    }

    @PutMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel order as admin")
    @PreAuthorize("@authorizationService.hasAnyPermission('ORDER_MANAGE_ALL', 'ORDER_CANCEL_ALL')")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelAdminOrder(
            @PathVariable Long orderId, @Valid @RequestBody OrderCancelRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cancel admin order successful", orderService.cancelAdminOrder(orderId, request)));
    }

    @PutMapping("/{orderId}/mark-refunded")
    @Operation(summary = "Mark order as manually refunded")
    @PreAuthorize("@authorizationService.hasAnyPermission('ORDER_MANAGE_ALL', 'REFUND_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<OrderResponse>> markRefunded(
            @PathVariable Long orderId, @Valid @RequestBody ManualRefundRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Mark order refunded successful", orderService.markRefunded(orderId, request)));
    }
}
