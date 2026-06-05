package com.tien.aivirabackend.domain.dto.response;

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
public class DashboardOrdersResponse {
    @Builder.Default
    List<OrderStatusCountResponse> statusCounts = new ArrayList<>();
}
