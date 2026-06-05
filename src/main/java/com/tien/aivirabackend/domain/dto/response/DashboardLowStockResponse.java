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
@Schema(description = "Admin dashboard low-stock active books response.")
public class DashboardLowStockResponse {
    @Schema(description = "Active books whose stock is less than or equal to the requested threshold.")
    @Builder.Default
    List<LowStockBookResponse> books = new ArrayList<>();
}
