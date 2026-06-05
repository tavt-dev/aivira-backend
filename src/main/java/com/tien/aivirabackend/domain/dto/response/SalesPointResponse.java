package com.tien.aivirabackend.domain.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Daily dashboard sales point.")
public class SalesPointResponse {
    @Schema(example = "2026-06-05")
    LocalDate date;
    @Schema(example = "1250000")
    BigDecimal revenue;
    @Schema(example = "8")
    Long orderCount;
}
