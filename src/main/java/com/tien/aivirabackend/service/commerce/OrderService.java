package com.tien.aivirabackend.service.commerce;

import java.time.Instant;

import com.tien.aivirabackend.constant.OrderStatus;
import com.tien.aivirabackend.constant.PaymentStatus;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.ManualRefundRequest;
import com.tien.aivirabackend.domain.dto.request.OrderCancelRequest;
import com.tien.aivirabackend.domain.dto.response.OrderResponse;
import com.tien.aivirabackend.domain.dto.response.OrderSummaryResponse;

public interface OrderService {
    PageResponse<OrderSummaryResponse> getMyOrders(OrderStatus status, int page, int size);

    OrderResponse getMyOrder(Long orderId);

    OrderResponse cancelMyOrder(Long orderId, OrderCancelRequest request);

    PageResponse<OrderSummaryResponse> getAdminOrders(OrderStatus status, PaymentStatus paymentStatus, String keyword, Instant fromDate,
            Instant toDate, int page, int size);

    OrderResponse getAdminOrder(Long orderId);

    OrderResponse confirmOrder(Long orderId);

    OrderResponse markPacking(Long orderId);

    OrderResponse markShipping(Long orderId);

    OrderResponse markCompleted(Long orderId);

    OrderResponse cancelAdminOrder(Long orderId, OrderCancelRequest request);

    OrderResponse markRefunded(Long orderId, ManualRefundRequest request);
}
