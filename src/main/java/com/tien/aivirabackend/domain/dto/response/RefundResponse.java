package com.tien.aivirabackend.domain.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.tien.aivirabackend.constant.RefundStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Manual full-order refund metadata. Provider refund APIs are not called in Phase 5.")
public class RefundResponse {
    @Schema(example = "15")
    Long id;
    @Schema(example = "RF-20260605-000001")
    String refundCode;
    @Schema(example = "120")
    Long orderId;
    @Schema(example = "ORD-20260605-ABC123")
    String orderCode;
    @Schema(example = "320000")
    BigDecimal amount;
    @Schema(example = "Customer requested cancellation before shipping")
    String reason;
    String note;
    @Schema(example = "COMPLETED")
    RefundStatus status;
    String refundedBy;
    Instant refundedAt;
    Instant createdAt;
    Instant updatedAt;
}
