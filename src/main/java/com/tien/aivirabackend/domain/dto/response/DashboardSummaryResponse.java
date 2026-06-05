package com.tien.aivirabackend.domain.dto.response;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Admin dashboard summary metrics for a date range.")
public class DashboardSummaryResponse {
    @Schema(description = "Revenue from successful payments.", example = "12500000")
    BigDecimal revenue;
    @Schema(example = "42")
    Long orderCount;
    @Schema(example = "35")
    Long successfulPaymentCount;
    @Schema(example = "3")
    Long failedPaymentCount;
    @Schema(example = "18")
    Long newUserCount;
    @Schema(example = "7")
    Long pendingOrderCount;
    @Schema(example = "4")
    Long pendingPaymentCount;
    @Schema(example = "6")
    Long lowStockCount;
}
