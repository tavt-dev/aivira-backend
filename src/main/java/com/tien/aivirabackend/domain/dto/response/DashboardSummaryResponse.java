package com.tien.aivirabackend.domain.dto.response;

import java.math.BigDecimal;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardSummaryResponse {
    BigDecimal revenue;
    Long orderCount;
    Long successfulPaymentCount;
    Long failedPaymentCount;
    Long newUserCount;
    Long pendingOrderCount;
    Long pendingPaymentCount;
    Long lowStockCount;
}
