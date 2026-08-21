package com.tien.aivirabackend.service.notification;

import java.time.Duration;
import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tien.aivirabackend.config.properties.NotificationProperties;
import com.tien.aivirabackend.repository.NotificationOutboxRepository;
import com.tien.aivirabackend.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "NOTIFICATION-CLEANUP")
public class NotificationCleanupService {
    private final NotificationRepository notificationRepository;
    private final NotificationOutboxRepository outboxRepository;
    private final NotificationProperties properties;

    @Scheduled(cron = "${notification.retention.cleanup-cron:0 40 3 * * *}")
    @Transactional
    public void cleanup() {
        Instant now = Instant.now();
        int outbox = outboxRepository.deleteCompletedBefore(
                now.minus(Duration.ofDays(properties.getRetention().getCompletedOutboxDays())));
        int read = notificationRepository
                .deleteReadBefore(now.minus(Duration.ofDays(properties.getRetention().getReadDays())));
        int unread = notificationRepository
                .deleteUnreadBefore(now.minus(Duration.ofDays(properties.getRetention().getUnreadDays())));
        if (outbox + read + unread > 0) {
            log.info("Notification cleanup completed outbox={} read={} unread={}", outbox, read, unread);
        }
    }
}
