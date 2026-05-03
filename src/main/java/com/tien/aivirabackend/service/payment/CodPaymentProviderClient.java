package com.tien.aivirabackend.service.payment;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.tien.aivirabackend.constant.PaymentMethod;
import com.tien.aivirabackend.constant.PaymentStatus;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentAttempt;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.PaymentErrorCode;

@Component
public class CodPaymentProviderClient implements PaymentProviderClient {
    @Override
    public PaymentMethod method() {
        return PaymentMethod.COD;
    }

    @Override
    public PaymentProviderResult createPayment(PaymentProviderRequest request) {
        return new PaymentProviderResult(
                request.paymentGroup().getPaymentCode(), null, null, null, null, null, null, null);
    }

    @Override
    public boolean verifyCallback(Map<String, ?> payload) {
        return true;
    }

    @Override
    public PaymentProviderCallbackResult parseCallback(Map<String, ?> payload) {
        throw new AppException(PaymentErrorCode.PAYMENT_PROVIDER_ERROR);
    }

    @Override
    public PaymentProviderQueryResult queryPayment(PaymentAttempt attempt) {
        return new PaymentProviderQueryResult(
                attempt.getProviderTxnRef(),
                attempt.getRequestId(),
                attempt.getProviderTransactionId(),
                attempt.getAmount(),
                PaymentStatus.PENDING,
                "COD payment does not support provider query",
                null);
    }
}
