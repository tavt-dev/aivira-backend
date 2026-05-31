package com.tien.aivirabackend.service.payment;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentExpiryScheduler {
    PaymentService paymentService;

    @Scheduled(fixedDelayString = "${payment.expiry-scan-delay-ms:60000}")
    public void expirePendingPayments() {
        paymentService.expirePendingPayments();
    }
}
