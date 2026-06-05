package com.tien.aivirabackend.domain.dto.response;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Top-selling book aggregate row for admin dashboard.")
public class TopBookResponse {
    @Schema(example = "101")
    Long productId;
    @Schema(example = "Clean Code")
    String productName;
    @Schema(example = "BOOK-CLN-CODE-PB")
    String sku;
    String thumbnailUrl;
    @Schema(example = "42")
    Long quantitySold;
    @Schema(example = "13440000")
    BigDecimal revenue;
}
