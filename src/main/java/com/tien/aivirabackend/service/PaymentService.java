package com.tien.aivirabackend.service;

import java.util.Map;

import com.tien.aivirabackend.domain.dto.response.PaymentGroupResponse;
import com.tien.aivirabackend.domain.dto.response.PaymentResponse;

public interface PaymentService {
    PaymentGroupResponse getMyPaymentGroup(String paymentGroupCode);

    PaymentResponse getMyPayment(Long paymentId);

    PaymentGroupResponse retry(String paymentGroupCode);

    PaymentGroupResponse handleVnpayCallback(Map<String, String> params, boolean returnRequest);

    PaymentGroupResponse handleMomoIpn(Map<String, Object> payload);

    void expirePendingPayments();
}
