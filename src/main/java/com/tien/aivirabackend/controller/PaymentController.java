package com.tien.aivirabackend.controller;

import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.domain.dto.response.PaymentGroupResponse;
import com.tien.aivirabackend.domain.dto.response.PaymentResponse;
import com.tien.aivirabackend.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@Tag(name = "Payment")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentController {
    PaymentService paymentService;

    @GetMapping("/payments/groups/{paymentGroupCode}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get my payment group")
    @PreAuthorize("@authorizationService.hasPermission('PAYMENT_READ_SELF')")
    public ResponseEntity<ApiResponse<PaymentGroupResponse>> getPaymentGroup(@PathVariable String paymentGroupCode) {
        return ResponseEntity.ok(ApiResponse.success(
                "Get payment group successful", paymentService.getMyPaymentGroup(paymentGroupCode)));
    }

    @GetMapping("/payments/{paymentId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get my payment")
    @PreAuthorize("@authorizationService.hasPermission('PAYMENT_READ_SELF')")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(@PathVariable Long paymentId) {
        return ResponseEntity.ok(ApiResponse.success("Get payment successful", paymentService.getMyPayment(paymentId)));
    }

    @PostMapping("/payments/groups/{paymentGroupCode}/retry")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Retry online payment group")
    @PreAuthorize("@authorizationService.hasPermission('PAYMENT_RETRY_SELF')")
    public ResponseEntity<ApiResponse<PaymentGroupResponse>> retry(@PathVariable String paymentGroupCode) {
        return ResponseEntity.ok(
                ApiResponse.success("Retry payment successful", paymentService.retry(paymentGroupCode)));
    }

    @GetMapping("/payments/vnpay/return")
    @Operation(summary = "Handle VNPay return URL")
    public ResponseEntity<ApiResponse<PaymentGroupResponse>> vnpayReturn(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "VNPay return processed", paymentService.handleVnpayCallback(queryParams(request), true)));
    }

    @GetMapping("/payments/vnpay/ipn")
    @Operation(summary = "Handle VNPay IPN")
    public ResponseEntity<ApiResponse<PaymentGroupResponse>> vnpayIpn(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "VNPay IPN processed", paymentService.handleVnpayCallback(queryParams(request), false)));
    }

    @PostMapping("/payments/momo/ipn")
    @Operation(summary = "Handle MoMo IPN")
    public ResponseEntity<ApiResponse<PaymentGroupResponse>> momoIpn(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(ApiResponse.success("MoMo IPN processed", paymentService.handleMomoIpn(payload)));
    }

    private Map<String, String> queryParams(HttpServletRequest request) {
        return request.getParameterMap().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, entry -> entry.getValue().length == 0 ? "" : entry.getValue()[0]));
    }
}
