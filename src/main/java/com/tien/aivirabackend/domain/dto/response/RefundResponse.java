package com.tien.aivirabackend.domain.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.tien.aivirabackend.constant.RefundStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RefundResponse {
    Long id;
    String refundCode;
    Long orderId;
    String orderCode;
    BigDecimal amount;
    String reason;
    String note;
    RefundStatus status;
    String refundedBy;
    Instant refundedAt;
    Instant createdAt;
    Instant updatedAt;
}
