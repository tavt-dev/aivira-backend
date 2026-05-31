package com.tien.aivirabackend.service.payment.provider;

public record PaymentProviderResult(
        String providerTxnRef,
        String requestId,
        String providerTransactionId,
        String paymentUrl,
        String deeplink,
        String qrCodeUrl,
        String rawRequest,
        String rawResponse) {}
