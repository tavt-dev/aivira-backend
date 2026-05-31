package com.tien.aivirabackend.service.payment.provider;

import com.tien.aivirabackend.domain.dto.RequestMetadata;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentAttempt;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentGroup;

public record PaymentProviderRequest(
        PaymentGroup paymentGroup, PaymentAttempt paymentAttempt, RequestMetadata requestMetadata) {}
