package com.tien.aivirabackend.domain.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Admin request to replace a variation stock quantity.")
public class StockUpdateRequest {
    @Schema(example = "25")
    @NotNull
    @Min(0)
    Integer stockQuantity;
}
