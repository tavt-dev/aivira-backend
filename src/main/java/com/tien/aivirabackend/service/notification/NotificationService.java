package com.tien.aivirabackend.service.notification;

import java.time.Instant;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tien.aivirabackend.config.properties.NotificationProperties;
import com.tien.aivirabackend.constant.NotificationType;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.response.NotificationResponse;
import com.tien.aivirabackend.domain.dto.response.ReadAllNotificationsResponse;
import com.tien.aivirabackend.domain.dto.response.UnreadNotificationCountResponse;
import com.tien.aivirabackend.domain.entity.notification.Notification;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.NotificationErrorCode;
import com.tien.aivirabackend.repository.NotificationRepository;
import com.tien.aivirabackend.service.auth.CurrentUserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final CurrentUserService currentUserService;
    private final NotificationProperties properties;

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getInbox(Boolean read, NotificationType type, int page, int size) {
        if (page < 1 || size < 1 || size > properties.getPageSizeMax()) {
            throw new AppException(NotificationErrorCode.NOTIFICATION_INVALID_FILTER);
        }
        var pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return PageResponse.from(notificationRepository
                .findInbox(currentUserService.getCurrentUserId(), read, type, pageable).map(notificationMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse unreadCount() {
        return new UnreadNotificationCountResponse(
                notificationRepository.countByRecipient_IdAndReadAtIsNull(currentUserService.getCurrentUserId()));
    }

    @Transactional
    public NotificationResponse markRead(Long id) {
        Notification notification = notificationRepository.findByIdAndRecipient_Id(id,
                currentUserService.getCurrentUserId()).orElseThrow(
                        () -> new AppException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
        }
        return notificationMapper.toResponse(notification);
    }

    @Transactional
    public ReadAllNotificationsResponse markAllRead() {
        Instant now = Instant.now();
        int count = notificationRepository.markAllRead(currentUserService.getCurrentUserId(), now);
        return new ReadAllNotificationsResponse(count, now);
    }
}
