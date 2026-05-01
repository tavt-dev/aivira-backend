package com.tien.aivirabackend.domain.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartResponse {
    Long id;
    List<CartItemResponse> items;
    Integer totalItems;
    BigDecimal subtotal;
}
