package com.tien.aivirabackend.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tien.aivirabackend.config.properties.NotificationProperties;
import com.tien.aivirabackend.constant.NotificationType;
import com.tien.aivirabackend.constant.OrderStatus;
import com.tien.aivirabackend.domain.entity.notification.Notification;
import com.tien.aivirabackend.domain.entity.notification.NotificationOutbox;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.repository.NotificationOutboxRepository;
import com.tien.aivirabackend.repository.NotificationRepository;
import com.tien.aivirabackend.repository.UserRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OrderNotificationProducerTest {
    @Mock NotificationRepository notificationRepository;
    @Mock NotificationOutboxRepository outboxRepository;
    @Mock UserRepository userRepository;

    OrderNotificationProducer producer;

    @BeforeEach
    void setUp() {
        NotificationProperties properties = new NotificationProperties();
        NotificationMapper mapper = new NotificationMapper(new ObjectMapper());
        producer = new OrderNotificationProducer(notificationRepository, outboxRepository, userRepository, mapper,
                properties, new SimpleMeterRegistry());
        lenient().when(userRepository.getReferenceById(anyString()))
                .thenAnswer(invocation -> User.builder().id(invocation.getArgument(0)).build());
        lenient().when(notificationRepository.save(any())).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(10L);
            return notification;
        });
    }

    @Test
    void orderCreated_shouldCreateCustomerAndDistinctAdminNotifications() {
        when(userRepository.findActiveUserIdsWithRolePermissions(anySet())).thenReturn(List.of("admin-1"));
        when(userRepository.findActiveUserIdsWithDirectPermissions(anySet(), any(Instant.class)))
                .thenReturn(List.of("admin-1", "admin-2"));

        producer.orderCreated(20L, "ORD20", "customer-1", OrderStatus.PENDING_CONFIRMATION);

        ArgumentCaptor<Notification> notifications = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(3)).save(notifications.capture());
        assertThat(notifications.getAllValues()).extracting(Notification::getType)
                .containsExactly(NotificationType.ORDER_CREATED, NotificationType.ADMIN_NEW_ORDER,
                        NotificationType.ADMIN_NEW_ORDER);
        assertThat(notifications.getAllValues()).extracting(n -> n.getRecipient().getId())
                .containsExactly("customer-1", "admin-1", "admin-2");
        verify(outboxRepository, times(3)).save(any(NotificationOutbox.class));
    }

    @Test
    void cancelled_shouldUseFallbackWhenReasonMissing() {
        producer.adminStatusUpdated(new OrderNotificationEvent("event", 20L, "ORD20", "customer-1",
                OrderStatus.CONFIRMED, OrderStatus.CANCELLED, OrderNotificationAction.ADMIN_STATUS_UPDATED, null,
                "admin-1", Instant.now()));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.ORDER_CANCELLED);
        assertThat(captor.getValue().getMessage()).doesNotContain("null").contains("ORD20");
    }

    @Test
    void duplicateEvent_shouldBeIgnored() {
        when(outboxRepository.existsByEventKey("ORDER:20:ORDER_COMPLETED:customer-1")).thenReturn(true);

        producer.adminStatusUpdated(new OrderNotificationEvent("event", 20L, "ORD20", "customer-1",
                OrderStatus.SHIPPING, OrderStatus.COMPLETED, OrderNotificationAction.ADMIN_STATUS_UPDATED, null,
                "admin-1", Instant.now()));

        verifyNoInteractions(notificationRepository);
        verify(outboxRepository, never()).save(any());
    }
}
