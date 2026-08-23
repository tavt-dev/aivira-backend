package com.tien.aivirabackend.controller;

import java.time.Instant;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.tien.aivirabackend.constant.OrderStatus;
import com.tien.aivirabackend.constant.PaymentStatus;
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
    @Operation(summary = "List orders for admin", description = "Lists all bookstore orders with status, keyword, date range, and pagination filters.")
    @PreAuthorize("@authorizationService.hasAnyPermission('ORDER_MANAGE_ALL', 'ORDER_READ_ALL')")
    public ResponseEntity<ApiResponse<PageResponse<OrderSummaryResponse>>> getAdminOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success("Get admin orders successful",
                orderService.getAdminOrders(status, paymentStatus, keyword, fromDate, toDate, page, size)));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get admin order detail")
    @PreAuthorize("@authorizationService.hasAnyPermission('ORDER_MANAGE_ALL', 'ORDER_READ_ALL')")
    public ResponseEntity<ApiResponse<OrderResponse>> getAdminOrder(@PathVariable Long orderId) {
        return ResponseEntity
                .ok(ApiResponse.success("Get admin order successful", orderService.getAdminOrder(orderId)));
    }

    @PutMapping("/{orderId}/confirm")
    @Operation(summary = "Confirm order", description = "Allowed from PENDING_CONFIRMATION or PAID to CONFIRMED.")
    @PreAuthorize("@authorizationService.hasAnyPermission('ORDER_MANAGE_ALL', 'ORDER_UPDATE_STATUS_ALL')")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success("Confirm order successful", orderService.confirmOrder(orderId)));
    }

    @PutMapping("/{orderId}/packing")
    @Operation(summary = "Mark order as packing", description = "Allowed from CONFIRMED to PACKING.")
    @PreAuthorize("@authorizationService.hasAnyPermission('ORDER_MANAGE_ALL', 'ORDER_UPDATE_STATUS_ALL')")
    public ResponseEntity<ApiResponse<OrderResponse>> markPacking(@PathVariable Long orderId) {
        return ResponseEntity
                .ok(ApiResponse.success("Mark order packing successful", orderService.markPacking(orderId)));
    }

    @PutMapping("/{orderId}/shipping")
    @Operation(summary = "Mark order as shipping", description = "Allowed from PACKING to SHIPPING.")
    @PreAuthorize("@authorizationService.hasAnyPermission('ORDER_MANAGE_ALL', 'ORDER_UPDATE_STATUS_ALL')")
    public ResponseEntity<ApiResponse<OrderResponse>> markShipping(@PathVariable Long orderId) {
        return ResponseEntity
                .ok(ApiResponse.success("Mark order shipping successful", orderService.markShipping(orderId)));
    }

    @PutMapping("/{orderId}/completed")
    @Operation(summary = "Mark order as completed", description = "Allowed from SHIPPING to COMPLETED.")
    @PreAuthorize("@authorizationService.hasAnyPermission('ORDER_MANAGE_ALL', 'ORDER_UPDATE_STATUS_ALL')")
    public ResponseEntity<ApiResponse<OrderResponse>> markCompleted(@PathVariable Long orderId) {
        return ResponseEntity
                .ok(ApiResponse.success("Mark order completed successful", orderService.markCompleted(orderId)));
    }

    @PutMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel order as admin", description = "Cancels safe pre-shipping orders and restores stock. Paid cancellation is handled by manual refund flow.")
    @PreAuthorize("@authorizationService.hasAnyPermission('ORDER_MANAGE_ALL', 'ORDER_CANCEL_ALL')")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelAdminOrder(@PathVariable Long orderId,
            @Valid @RequestBody OrderCancelRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Cancel admin order successful", orderService.cancelAdminOrder(orderId, request)));
    }

    @PutMapping("/{orderId}/mark-refunded")
    @Operation(summary = "Mark order as manually refunded", description = "Records a full manual refund for a paid pre-shipping order without calling VNPay or MoMo refund APIs.")
    @PreAuthorize("@authorizationService.hasAnyPermission('ORDER_MANAGE_ALL', 'REFUND_MANAGE_ALL')")
    public ResponseEntity<ApiResponse<OrderResponse>> markRefunded(@PathVariable Long orderId,
            @Valid @RequestBody ManualRefundRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("Mark order refunded successful", orderService.markRefunded(orderId, request)));
    }
}
