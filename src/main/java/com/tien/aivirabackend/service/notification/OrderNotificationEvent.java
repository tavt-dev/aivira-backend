package com.tien.aivirabackend.service.notification;

import java.time.Instant;

import com.tien.aivirabackend.constant.OrderStatus;

public record OrderNotificationEvent(String eventId, Long orderId, String orderCode, String customerUserId,
        OrderStatus previousStatus, OrderStatus currentStatus, OrderNotificationAction action, String cancelReason,
        String actorUserId, Instant occurredAt) {
}
