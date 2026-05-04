package com.tien.aivirabackend.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.tien.aivirabackend.constant.OrderStatus;
import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.OrderCancelRequest;
import com.tien.aivirabackend.domain.dto.response.OrderResponse;
import com.tien.aivirabackend.domain.dto.response.OrderSummaryResponse;
import com.tien.aivirabackend.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/orders")
@Tag(name = "Customer Orders")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderController {
    OrderService orderService;

    @GetMapping
    @Operation(summary = "List current customer orders")
    @PreAuthorize("@authorizationService.hasPermission('ORDER_READ_SELF')")
    public ResponseEntity<ApiResponse<PageResponse<OrderSummaryResponse>>> getMyOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                ApiResponse.success("Get orders successful", orderService.getMyOrders(status, page, size)));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get current customer order detail")
    @PreAuthorize("@authorizationService.hasPermission('ORDER_READ_SELF')")
    public ResponseEntity<ApiResponse<OrderResponse>> getMyOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success("Get order successful", orderService.getMyOrder(orderId)));
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel current customer order")
    @PreAuthorize("@authorizationService.hasPermission('ORDER_CANCEL_SELF')")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelMyOrder(
            @PathVariable Long orderId, @Valid @RequestBody OrderCancelRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Cancel order successful", orderService.cancelMyOrder(orderId, request)));
    }
}
