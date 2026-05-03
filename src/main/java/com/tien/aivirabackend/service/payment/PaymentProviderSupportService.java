package com.tien.aivirabackend.service.payment;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

import com.tien.aivirabackend.constant.PaymentMethod;
import com.tien.aivirabackend.constant.PaymentProvider;
import com.tien.aivirabackend.constant.PaymentStatus;
import com.tien.aivirabackend.domain.dto.RequestMetadata;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentAttempt;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentGroup;
import com.tien.aivirabackend.repository.PaymentAttemptRepository;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentProviderSupportService {
    PaymentAttemptRepository paymentAttemptRepository;
    List<PaymentProviderClient> paymentProviderClients;
    MeterRegistry meterRegistry;

    public PaymentProviderClient provider(
            PaymentMethod method, Supplier<? extends RuntimeException> exceptionSupplier) {
        return paymentProviderClients.stream()
                .filter(client -> client.method() == method)
                .findFirst()
                .orElseThrow(exceptionSupplier);
    }

    public PaymentAttempt createAttempt(PaymentGroup paymentGroup) {
        int attemptNo = paymentAttemptRepository.countByPaymentGroupId(paymentGroup.getId()) + 1;
        String suffix = "A" + attemptNo + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String providerRef = paymentGroup.getMethod() == PaymentMethod.COD
                ? paymentGroup.getPaymentCode()
                : paymentGroup.getPaymentCode() + "-" + suffix;
        return paymentAttemptRepository.save(PaymentAttempt.builder()
                .paymentGroup(paymentGroup)
                .provider(PaymentProvider.valueOf(paymentGroup.getMethod().name()))
                .method(paymentGroup.getMethod())
                .attemptNo(attemptNo)
                .providerTxnRef(providerRef)
                .requestId(providerRef + "-REQ")
                .status(PaymentStatus.PENDING)
                .amount(paymentGroup.getAmount())
                .expiresAt(paymentGroup.getExpiresAt())
                .build());
    }

    public PaymentProviderResult createPaymentWithMetrics(
            PaymentProviderClient provider,
            PaymentGroup paymentGroup,
            PaymentAttempt attempt,
            RequestMetadata requestMetadata) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            PaymentProviderResult result =
                    provider.createPayment(new PaymentProviderRequest(paymentGroup, attempt, requestMetadata));
            increment(
                    "payment_provider_create_total",
                    "method",
                    paymentGroup.getMethod().name(),
                    "status",
                    "SUCCESS");
            return result;
        } catch (RuntimeException ex) {
            increment(
                    "payment_provider_create_total",
                    "method",
                    paymentGroup.getMethod().name(),
                    "status",
                    "FAILED");
            throw ex;
        } finally {
            sample.stop(meterRegistry.timer(
                    "payment_provider_request_seconds",
                    "provider",
                    provider.method().name(),
                    "operation",
                    "create"));
        }
    }

    public PaymentProviderQueryResult queryPaymentWithMetrics(PaymentProviderClient provider, PaymentAttempt attempt) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return provider.queryPayment(attempt);
        } finally {
            sample.stop(meterRegistry.timer(
                    "payment_provider_request_seconds",
                    "provider",
                    provider.method().name(),
                    "operation",
                    "query"));
        }
    }

    public void applyProviderResult(PaymentGroup paymentGroup, PaymentAttempt attempt, PaymentProviderResult result) {
        paymentGroup.setProviderTxnRef(result.providerTxnRef());
        paymentGroup.setProviderTransactionId(result.providerTransactionId());
        paymentGroup.setPaymentUrl(result.paymentUrl());
        paymentGroup.setDeeplink(result.deeplink());
        paymentGroup.setQrCodeUrl(result.qrCodeUrl());
        paymentGroup.setRawResponse(result.rawResponse());
        attempt.setProviderTxnRef(result.providerTxnRef());
        attempt.setRequestId(result.requestId());
        attempt.setProviderTransactionId(result.providerTransactionId());
        attempt.setPaymentUrl(result.paymentUrl());
        attempt.setDeeplink(result.deeplink());
        attempt.setQrCodeUrl(result.qrCodeUrl());
        attempt.setRawRequest(result.rawRequest());
        attempt.setRawResponse(result.rawResponse());
    }

    private void increment(String metricName, String... tags) {
        meterRegistry.counter(metricName, tags).increment();
    }
}
