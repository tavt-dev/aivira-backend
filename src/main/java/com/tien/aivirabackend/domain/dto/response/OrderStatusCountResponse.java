package com.tien.aivirabackend.domain.dto.response;

import com.tien.aivirabackend.constant.OrderStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Order count grouped by status.")
public class OrderStatusCountResponse {
    @Schema(example = "PENDING_CONFIRMATION")
    OrderStatus status;

    @Schema(example = "7")
    Long count;
}
