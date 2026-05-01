package com.tien.aivirabackend.service.payment;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.tien.aivirabackend.constant.PaymentMethod;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentGroup;

@Component
public class CodPaymentProviderClient implements PaymentProviderClient {
    @Override
    public PaymentMethod method() {
        return PaymentMethod.COD;
    }

    @Override
    public PaymentProviderResult createPayment(PaymentGroup paymentGroup) {
        return new PaymentProviderResult(paymentGroup.getPaymentCode(), null, null, null, null, null);
    }

    @Override
    public boolean verifyCallback(Map<String, ?> payload) {
        return true;
    }
}
