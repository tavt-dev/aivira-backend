package com.tien.aivirabackend.service.payment.provider;

import java.math.BigDecimal;
import java.util.Map;

import com.tien.aivirabackend.constant.PaymentStatus;

public record PaymentProviderCallbackResult(
        String providerTxnRef,
        String requestId,
        String transactionId,
        BigDecimal amount,
        PaymentStatus status,
        String eventKey,
        Map<String, ?> rawPayload) {}
