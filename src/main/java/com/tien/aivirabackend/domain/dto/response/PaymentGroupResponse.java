package com.tien.aivirabackend.domain.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

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
public class PaymentGroupResponse {
    String paymentCode;
    PaymentMethod method;
    PaymentStatus status;
    BigDecimal amount;
    String providerTxnRef;
    String providerTransactionId;
    String paymentUrl;
    String deeplink;
    String qrCodeUrl;
    Instant expiresAt;
    Instant paidAt;
    List<PaymentResponse> payments;
    List<OrderResponse> orders;
}
