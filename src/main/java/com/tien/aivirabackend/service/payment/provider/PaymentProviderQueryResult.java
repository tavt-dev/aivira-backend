package com.tien.aivirabackend.service.payment.provider;

import java.math.BigDecimal;

import com.tien.aivirabackend.constant.PaymentStatus;

public record PaymentProviderQueryResult(
        String providerTxnRef,
        String requestId,
        String transactionId,
        BigDecimal amount,
        PaymentStatus status,
        String message,
        String rawResponse) {}
