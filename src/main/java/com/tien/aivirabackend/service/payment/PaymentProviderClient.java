package com.tien.aivirabackend.service.payment;

import java.util.Map;

import com.tien.aivirabackend.constant.PaymentMethod;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentGroup;

public interface PaymentProviderClient {
    PaymentMethod method();

    PaymentProviderResult createPayment(PaymentGroup paymentGroup);

    boolean verifyCallback(Map<String, ?> payload);
}
