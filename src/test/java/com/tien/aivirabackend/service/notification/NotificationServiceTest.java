package com.tien.aivirabackend.service.notification;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tien.aivirabackend.config.properties.NotificationProperties;
import com.tien.aivirabackend.constant.NotificationType;
import com.tien.aivirabackend.domain.entity.notification.Notification;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.NotificationErrorCode;
import com.tien.aivirabackend.repository.NotificationRepository;
import com.tien.aivirabackend.service.auth.CurrentUserService;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock NotificationRepository repository;
    @Mock CurrentUserService currentUserService;
    NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(repository, new NotificationMapper(new ObjectMapper()), currentUserService,
                new NotificationProperties());
        lenient().when(currentUserService.getCurrentUserId()).thenReturn("user-1");
    }

    @Test
    void markRead_shouldBeIdempotentAndScopedToCurrentUser() {
        Notification notification = Notification.builder().type(NotificationType.ORDER_CONFIRMED).title("title")
                .message("message").payload("{}").build();
        when(repository.findByIdAndRecipient_Id(10L, "user-1")).thenReturn(Optional.of(notification));

        var first = service.markRead(10L);
        Instant readAt = first.getReadAt();
        var second = service.markRead(10L);

        assertThat(first.isRead()).isTrue();
        assertThat(second.getReadAt()).isEqualTo(readAt);
        verify(repository, times(2)).findByIdAndRecipient_Id(10L, "user-1");
    }

    @Test
    void markRead_shouldHideOtherUsersNotification() {
        when(repository.findByIdAndRecipient_Id(10L, "user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(10L)).isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
    }

    @Test
    void getInbox_shouldRejectOversizedPage() {
        assertThatThrownBy(() -> service.getInbox(null, null, 1, 101)).isInstanceOf(AppException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void unreadCount_shouldUseCurrentUserOnly() {
        when(repository.countByRecipient_IdAndReadAtIsNull("user-1")).thenReturn(4L);
        assertThat(service.unreadCount().unreadCount()).isEqualTo(4L);
    }
}
