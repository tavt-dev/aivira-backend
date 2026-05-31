package com.tien.aivirabackend.controller;

import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.domain.dto.response.PaymentGroupResponse;
import com.tien.aivirabackend.domain.dto.response.PaymentReconciliationResponse;
import com.tien.aivirabackend.domain.dto.response.PaymentResponse;
import com.tien.aivirabackend.domain.dto.response.VnpayIpnResponse;
import com.tien.aivirabackend.service.payment.PaymentService;
import com.tien.aivirabackend.service.auth.RequestMetadataService;

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
    RequestMetadataService requestMetadataService;

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
    public ResponseEntity<ApiResponse<PaymentGroupResponse>> retry(
            @PathVariable String paymentGroupCode, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Retry payment successful",
                paymentService.retry(paymentGroupCode, requestMetadataService.from(request))));
    }

    @GetMapping("/payments/vnpay/return")
    @Operation(summary = "Handle VNPay return URL")
    public ResponseEntity<ApiResponse<PaymentGroupResponse>> vnpayReturn(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "VNPay return processed", paymentService.handleVnpayCallback(queryParams(request), true)));
    }

    @GetMapping("/payments/vnpay/ipn")
    @Operation(summary = "Handle VNPay IPN")
    public ResponseEntity<VnpayIpnResponse> vnpayIpn(HttpServletRequest request) {
        return ResponseEntity.ok(paymentService.handleVnpayIpn(queryParams(request)));
    }

    @PostMapping("/payments/momo/ipn")
    @Operation(summary = "Handle MoMo IPN")
    public ResponseEntity<Void> momoIpn(@RequestBody Map<String, Object> payload) {
        paymentService.handleMomoIpn(payload);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/payments/groups/{paymentGroupCode}/reconcile")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Reconcile payment group with provider")
    @PreAuthorize("@authorizationService.hasPermission('PAYMENT_RECONCILE')")
    public ResponseEntity<ApiResponse<PaymentReconciliationResponse>> reconcile(@PathVariable String paymentGroupCode) {
        return ResponseEntity.ok(
                ApiResponse.success("Reconcile payment successful", paymentService.reconcile(paymentGroupCode)));
    }

    private Map<String, String> queryParams(HttpServletRequest request) {
        return request.getParameterMap().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, entry -> entry.getValue().length == 0 ? "" : entry.getValue()[0]));
    }
}
