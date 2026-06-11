package com.tien.aivirabackend.service.payment;

import java.util.Map;

import com.tien.aivirabackend.domain.dto.RequestMetadata;
import com.tien.aivirabackend.domain.dto.response.PaymentGroupResponse;
import com.tien.aivirabackend.domain.dto.response.PaymentReconciliationResponse;
import com.tien.aivirabackend.domain.dto.response.PaymentResponse;
import com.tien.aivirabackend.domain.dto.response.VnpayIpnResponse;

public interface PaymentService {
    PaymentGroupResponse getMyPaymentGroup(String paymentGroupCode);

    PaymentGroupResponse getAdminPaymentGroup(String paymentGroupCode);

    PaymentResponse getMyPayment(Long paymentId);

    PaymentGroupResponse retry(String paymentGroupCode, RequestMetadata requestMetadata);

    PaymentGroupResponse handleVnpayCallback(Map<String, String> params, boolean returnRequest);

    VnpayIpnResponse handleVnpayIpn(Map<String, String> params);

    PaymentGroupResponse handleMomoIpn(Map<String, Object> payload);

    PaymentReconciliationResponse reconcile(String paymentGroupCode);

    void expirePendingPayments();
}
