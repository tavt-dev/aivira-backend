package com.tien.aivirabackend.service;

import com.tien.aivirabackend.constant.OrderStatus;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.OrderCancelRequest;
import com.tien.aivirabackend.domain.dto.response.OrderResponse;
import com.tien.aivirabackend.domain.dto.response.OrderSummaryResponse;

public interface OrderService {
    PageResponse<OrderSummaryResponse> getMyOrders(OrderStatus status, int page, int size);

    OrderResponse getMyOrder(Long orderId);

    OrderResponse cancelMyOrder(Long orderId, OrderCancelRequest request);
}
