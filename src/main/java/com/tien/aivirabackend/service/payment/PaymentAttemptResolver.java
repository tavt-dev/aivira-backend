package com.tien.aivirabackend.service.payment;

import org.springframework.stereotype.Service;

import com.tien.aivirabackend.constant.PaymentProvider;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentAttempt;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.PaymentErrorCode;
import com.tien.aivirabackend.repository.PaymentAttemptRepository;
import com.tien.aivirabackend.repository.PaymentGroupRepository;
import com.tien.aivirabackend.service.payment.provider.PaymentProviderCallbackResult;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentAttemptResolver {
    PaymentAttemptRepository paymentAttemptRepository;
    PaymentGroupRepository paymentGroupRepository;

    public PaymentAttempt resolveForUpdate(PaymentProvider provider, PaymentProviderCallbackResult callbackResult) {
        var attempt = java.util.Optional.<PaymentAttempt> empty();
        if (hasText(callbackResult.providerTxnRef())) {
            attempt = paymentAttemptRepository.findByProviderAndProviderTxnRefForUpdate(provider,
                    callbackResult.providerTxnRef());
        }
        if (attempt.isEmpty() && hasText(callbackResult.requestId())) {
            attempt = paymentAttemptRepository.findByRequestIdForUpdate(callbackResult.requestId());
        }
        if (attempt.isPresent()) {
            return attempt.get();
        }
        if (hasText(callbackResult.providerTxnRef())) {
            return paymentGroupRepository.findByProviderTxnRef(callbackResult.providerTxnRef())
                    .map(group -> paymentAttemptRepository.save(PaymentAttempt.builder().paymentGroup(group)
                            .provider(provider).method(group.getMethod()).attemptNo(1)
                            .providerTxnRef(group.getProviderTxnRef()).requestId(callbackResult.requestId())
                            .status(group.getStatus()).amount(group.getAmount()).paymentUrl(group.getPaymentUrl())
                            .deeplink(group.getDeeplink()).qrCodeUrl(group.getQrCodeUrl())
                            .providerTransactionId(group.getProviderTransactionId()).rawResponse(group.getRawResponse())
                            .expiresAt(group.getExpiresAt()).completedAt(group.getPaidAt()).build()))
                    .orElseThrow(() -> new AppException(PaymentErrorCode.PAYMENT_GROUP_NOT_FOUND));
        }
        throw new AppException(PaymentErrorCode.PAYMENT_GROUP_NOT_FOUND);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
