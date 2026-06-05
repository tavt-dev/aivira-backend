package com.tien.aivirabackend.domain.dto.response;

import java.math.BigDecimal;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopBookResponse {
    Long productId;
    String productName;
    String sku;
    String thumbnailUrl;
    Long quantitySold;
    BigDecimal revenue;
}
