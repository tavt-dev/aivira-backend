package com.tien.aivirabackend.service.payment;

public record PaymentProviderResult(
        String providerTxnRef,
        String providerTransactionId,
        String paymentUrl,
        String deeplink,
        String qrCodeUrl,
        String rawResponse) {}
