package com.tien.aivirabackend.repository.projection;

import com.tien.aivirabackend.constant.OrderStatus;

public interface OrderStatusCountProjection {
    OrderStatus getStatus();

    Long getCount();
}
