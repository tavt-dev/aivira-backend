package com.tien.aivirabackend.domain.dto.response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardSalesResponse {
    Instant fromDate;
    Instant toDate;

    @Builder.Default
    List<SalesPointResponse> points = new ArrayList<>();
}
