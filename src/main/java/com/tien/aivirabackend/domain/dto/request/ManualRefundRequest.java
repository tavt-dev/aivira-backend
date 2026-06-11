package com.tien.aivirabackend.domain.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Admin request to mark a paid pre-shipping order as manually refunded.")
public class ManualRefundRequest {
    @Schema(
            description = "Full refund amount. Must match the successful payment/order payable amount in Phase 5.",
            example = "320000")
    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    BigDecimal amount;

    @Schema(example = "Customer requested cancellation before shipping")
    @NotBlank
    @Size(max = 255)
    String reason;

    @Schema(example = "Refund transferred manually via bank at 2026-06-05 10:30.")
    @NotBlank
    @Size(max = 1000)
    String note;
}
