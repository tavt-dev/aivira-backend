package com.tien.aivirabackend.domain.dto.response;

import com.tien.aivirabackend.constant.OrderStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderStatusCountResponse {
    OrderStatus status;
    Long count;
}
