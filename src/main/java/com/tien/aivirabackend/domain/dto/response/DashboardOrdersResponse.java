package com.tien.aivirabackend.domain.dto.response;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Admin dashboard order counts grouped by status.")
public class DashboardOrdersResponse {
    @Schema(description = "Order status count rows.")
    @Builder.Default
    List<OrderStatusCountResponse> statusCounts = new ArrayList<>();
}
