package com.tien.aivirabackend.domain.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ManualRefundRequest {
    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    BigDecimal amount;

    @NotBlank
    @Size(max = 255)
    String reason;

    @NotBlank
    @Size(max = 1000)
    String note;
}
