package com.tien.aivirabackend.domain.dto.request;

import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Order cancellation request for safe pre-shipping cancellation flows.")
public class OrderCancelRequest {
    @Schema(example = "Customer requested cancellation before packing.")
    @Size(max = 500)
    String reason;
}
