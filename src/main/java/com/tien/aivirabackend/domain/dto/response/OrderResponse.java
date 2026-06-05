package com.tien.aivirabackend.domain.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.tien.aivirabackend.constant.OrderStatus;
import com.tien.aivirabackend.constant.PaymentMethod;
import com.tien.aivirabackend.constant.PaymentStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Order detail response with item snapshots, payment status, shipping address, and optional refund metadata.")
public class OrderResponse {
    @Schema(example = "120")
    Long id;
    @Schema(example = "ORD-20260605-ABC123")
    String orderCode;
    @Schema(example = "320000")
    BigDecimal subtotal;
    @Schema(example = "0")
    BigDecimal shippingFee;
    @Schema(example = "30000")
    BigDecimal discountAmount;
    @Schema(example = "290000")
    BigDecimal totalAmount;
    String notes;
    String cancelReason;
    @Schema(example = "CONFIRMED")
    OrderStatus orderStatus;
    String paymentGroupCode;
    @Schema(example = "VNPAY")
    PaymentMethod paymentMethod;
    @Schema(example = "SUCCESS")
    PaymentStatus paymentStatus;
    Instant paidAt;
    @Schema(description = "Present only when a manual refund has been marked.")
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
