package com.tien.aivirabackend.domain.dto.response;

import java.time.Instant;
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
@Schema(description = "Admin dashboard daily sales series for a date range.")
public class DashboardSalesResponse {
    @Schema(example = "2026-05-06T00:00:00Z")
    Instant fromDate;

    @Schema(example = "2026-06-05T00:00:00Z")
    Instant toDate;

    @Schema(description = "Daily revenue and order count points.")
    @Builder.Default
    List<SalesPointResponse> points = new ArrayList<>();
}
