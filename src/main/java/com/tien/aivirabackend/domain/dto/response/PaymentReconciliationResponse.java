package com.tien.aivirabackend.domain.dto.response;

import java.time.Instant;

import com.tien.aivirabackend.constant.PaymentMethod;
import com.tien.aivirabackend.constant.PaymentStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentReconciliationResponse {
    String paymentGroupCode;
    PaymentMethod method;
    String providerTxnRef;
    PaymentStatus localStatusBefore;
    PaymentStatus localStatusAfter;
    PaymentStatus providerStatus;
    boolean changed;
    String message;
    Instant checkedAt;
}
