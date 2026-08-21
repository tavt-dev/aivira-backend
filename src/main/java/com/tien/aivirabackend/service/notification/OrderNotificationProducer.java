package com.tien.aivirabackend.service.notification;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tien.aivirabackend.config.properties.NotificationProperties;
import com.tien.aivirabackend.constant.*;
import com.tien.aivirabackend.domain.entity.notification.Notification;
import com.tien.aivirabackend.domain.entity.notification.NotificationOutbox;
import com.tien.aivirabackend.repository.NotificationOutboxRepository;
import com.tien.aivirabackend.repository.NotificationRepository;
import com.tien.aivirabackend.repository.UserRepository;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "ORDER-NOTIFICATION")
@Transactional(propagation = Propagation.MANDATORY)
public class OrderNotificationProducer {
    private static final Set<PermissionCode> ADMIN_ORDER_PERMISSIONS = Set.of(PermissionCode.ORDER_MANAGE_ALL,
            PermissionCode.ORDER_READ_ALL, PermissionCode.ORDER_UPDATE_STATUS_ALL);

    private final NotificationRepository notificationRepository;
    private final NotificationOutboxRepository outboxRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationProperties properties;
    private final MeterRegistry meterRegistry;

    public void orderCreated(Long orderId, String orderCode, String customerUserId, OrderStatus currentStatus) {
        if (!properties.isEnabled()) return;
        create(orderId, orderCode, customerUserId, NotificationType.ORDER_CREATED, null, currentStatus, null, false);

        Set<String> adminIds = new LinkedHashSet<>(
                userRepository.findActiveUserIdsWithRolePermissions(ADMIN_ORDER_PERMISSIONS));
        adminIds.addAll(userRepository.findActiveUserIdsWithDirectPermissions(ADMIN_ORDER_PERMISSIONS, Instant.now()));
        if (adminIds.isEmpty()) {
            log.warn("No active admin recipient for new order orderId={} orderCode={}", orderId, orderCode);
            meterRegistry.counter("notification.admin.recipient.missing").increment();
        }
        adminIds.forEach(adminId -> create(orderId, orderCode, adminId, NotificationType.ADMIN_NEW_ORDER, null,
                currentStatus, null, true));
    }

    public void adminStatusUpdated(OrderNotificationEvent event) {
        if (!properties.isEnabled()) return;
        NotificationType type = switch (event.currentStatus()) {
            case CONFIRMED -> NotificationType.ORDER_CONFIRMED;
            case PACKING -> NotificationType.ORDER_PACKING;
            case SHIPPING -> NotificationType.ORDER_SHIPPING;
            case COMPLETED -> NotificationType.ORDER_COMPLETED;
            case CANCELLED -> NotificationType.ORDER_CANCELLED;
            case REFUNDED -> NotificationType.ORDER_REFUNDED;
            default -> null;
        };
        if (type != null) {
            create(event.orderId(), event.orderCode(), event.customerUserId(), type, event.previousStatus(),
                    event.currentStatus(), event.cancelReason(), false);
        }
    }

    private void create(Long orderId, String orderCode, String recipientId, NotificationType type,
            OrderStatus previousStatus, OrderStatus currentStatus, String cancelReason, boolean admin) {
        String eventKey = "ORDER:%d:%s:%s".formatted(orderId, type, recipientId);
        if (outboxRepository.existsByEventKey(eventKey)) {
            log.debug("Skipping duplicate notification eventKey={}", eventKey);
            return;
        }
        Template template = template(type, orderCode, cancelReason);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", orderId);
        payload.put("orderCode", orderCode);
        if (previousStatus != null) payload.put("previousStatus", previousStatus.name());
        if (currentStatus != null) payload.put("currentStatus", currentStatus.name());
        if (cancelReason != null && !cancelReason.isBlank()) payload.put("cancelReason", cancelReason);

        Notification notification = notificationRepository.save(Notification.builder()
                .recipient(userRepository.getReferenceById(recipientId)).type(type).title(template.title())
                .message(template.message()).resourceType(NotificationResourceType.ORDER)
                .resourceId(String.valueOf(orderId)).actionUrl((admin ? "/admin/orders/" : "/orders/") + orderId)
                .payload(notificationMapper.writePayload(payload)).build());
        outboxRepository.save(NotificationOutbox.builder().eventKey(eventKey).notification(notification)
                .channel(NotificationChannel.IN_APP_REALTIME).status(NotificationOutboxStatus.PENDING)
                .attemptCount(0).nextAttemptAt(Instant.now()).build());
        meterRegistry.counter("notification.created", "type", type.name()).increment();
        log.info("Notification created eventKey={} notificationId={} type={} recipientUserId={} orderId={}",
                eventKey, notification.getId(), type, recipientId, orderId);
    }

    private Template template(NotificationType type, String code, String reason) {
        return switch (type) {
            case ORDER_CREATED -> new Template("Đặt hàng thành công", "Đơn hàng %s đã được tạo thành công.".formatted(code));
            case ADMIN_NEW_ORDER -> new Template("Có đơn hàng mới", "Đơn hàng %s vừa được tạo và đang chờ xử lý.".formatted(code));
            case ORDER_CONFIRMED -> new Template("Đơn hàng đã được xác nhận", "Đơn hàng %s đã được cửa hàng xác nhận.".formatted(code));
            case ORDER_PACKING -> new Template("Đơn hàng đang được đóng gói", "Cửa hàng đang chuẩn bị đơn hàng %s.".formatted(code));
            case ORDER_SHIPPING -> new Template("Đơn hàng đang được giao", "Đơn hàng %s đang trên đường giao đến bạn.".formatted(code));
            case ORDER_COMPLETED -> new Template("Đơn hàng đã hoàn tất", "Đơn hàng %s đã hoàn tất. Cảm ơn bạn đã mua hàng.".formatted(code));
            case ORDER_CANCELLED -> new Template("Đơn hàng đã bị hủy",
                    reason == null || reason.isBlank() ? "Đơn hàng %s đã bị hủy. Vui lòng liên hệ cửa hàng để biết thêm chi tiết.".formatted(code)
                            : "Đơn hàng %s đã bị hủy. Lý do: %s.".formatted(code, reason));
            case ORDER_REFUNDED -> new Template("Đơn hàng đã được hoàn tiền", "Đơn hàng %s đã được ghi nhận hoàn tiền.".formatted(code));
        };
    }

    private record Template(String title, String message) {}
}
