package com.tien.aivirabackend.repository.projection;

public interface DailyOrderCountProjection {
    Object getOrderDate();

    Long getOrderCount();
}
