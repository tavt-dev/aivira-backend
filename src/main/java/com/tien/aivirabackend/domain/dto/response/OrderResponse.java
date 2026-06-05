package com.tien.aivirabackend.domain.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

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
public class OrderResponse {
    Long id;
    String orderCode;
    BigDecimal subtotal;
    BigDecimal shippingFee;
    BigDecimal discountAmount;
    BigDecimal totalAmount;
    String notes;
    String cancelReason;
    OrderStatus orderStatus;
    String paymentGroupCode;
    PaymentMethod paymentMethod;
    PaymentStatus paymentStatus;
    Instant paidAt;
    RefundResponse refund;
    String shippingRecipientName;
    String shippingPhoneNumber;
    String shippingAddressLine;
    String shippingWard;
    String shippingDistrict;
    String shippingCity;
    List<OrderItemResponse> items;
    Instant createdAt;
    Instant updatedAt;
}
