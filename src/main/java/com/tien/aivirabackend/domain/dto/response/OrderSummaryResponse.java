package com.tien.aivirabackend.domain.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.tien.aivirabackend.constant.OrderStatus;
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
public class OrderSummaryResponse {
    Long id;
    String orderCode;
    Long shopId;
    String shopName;
    BigDecimal totalAmount;
    OrderStatus orderStatus;
    String cancelReason;
    String paymentGroupCode;
    PaymentMethod paymentMethod;
    PaymentStatus paymentStatus;
    Instant paidAt;
    Integer itemCount;
    Instant createdAt;
    Instant updatedAt;
}
