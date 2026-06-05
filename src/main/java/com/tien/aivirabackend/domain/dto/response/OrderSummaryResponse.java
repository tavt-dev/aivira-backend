package com.tien.aivirabackend.domain.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

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
@Schema(description = "Order summary for customer and admin order lists.")
public class OrderSummaryResponse {
    @Schema(example = "120")
    Long id;
    @Schema(example = "ORD-20260605-ABC123")
    String orderCode;
    @Schema(example = "320000")
    BigDecimal totalAmount;
    @Schema(example = "PENDING_CONFIRMATION")
    OrderStatus orderStatus;
    String cancelReason;
    String paymentGroupCode;
    @Schema(example = "COD")
    PaymentMethod paymentMethod;
    @Schema(example = "PENDING")
    PaymentStatus paymentStatus;
    Instant paidAt;
    Integer itemCount;
    Instant createdAt;
    Instant updatedAt;
}
