package com.tien.aivirabackend.service.payment;

import java.util.Map;

import com.tien.aivirabackend.constant.PaymentMethod;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentAttempt;

public interface PaymentProviderClient {
    PaymentMethod method();

    PaymentProviderResult createPayment(PaymentProviderRequest request);

    boolean verifyCallback(Map<String, ?> payload);

    PaymentProviderCallbackResult parseCallback(Map<String, ?> payload);

    PaymentProviderQueryResult queryPayment(PaymentAttempt attempt);
}
