package com.tien.aivirabackend.service.notification;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tien.aivirabackend.config.properties.NotificationProperties;
import com.tien.aivirabackend.constant.NotificationOutboxStatus;
import com.tien.aivirabackend.domain.entity.notification.NotificationOutbox;
import com.tien.aivirabackend.repository.NotificationOutboxRepository;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "NOTIFICATION-OUTBOX")
public class NotificationOutboxWorker {
    private static final long[] BACKOFF_SECONDS = { 5, 30, 120, 600, 1800 };
    private final NotificationOutboxRepository outboxRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationSseRegistry sseRegistry;
    private final NotificationProperties properties;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedDelayString = "${notification.outbox.poll-delay-ms:1000}")
    @Transactional
    public void dispatch() {
        if (!properties.isEnabled()) return;
        Timer.Sample sample = Timer.start(meterRegistry);
        Instant now = Instant.now();
        List<NotificationOutbox> batch = outboxRepository.lockDispatchBatch(now,
                PageRequest.of(0, properties.getOutbox().getBatchSize()));
        for (NotificationOutbox outbox : batch) {
            outbox.setStatus(NotificationOutboxStatus.PROCESSING);
            outbox.setLockedAt(now);
            try {
                var notification = outbox.getNotification();
                sseRegistry.publish(notification.getRecipient().getId(), notificationMapper.toResponse(notification));
                outbox.setStatus(NotificationOutboxStatus.COMPLETED);
                outbox.setProcessedAt(Instant.now());
                outbox.setLockedAt(null);
                outbox.setLastError(null);
                meterRegistry.counter("notification.outbox.processed", "status", "completed").increment();
            } catch (Exception ex) {
                fail(outbox, ex);
            }
        }
        sample.stop(meterRegistry.timer("notification.outbox.processing.duration"));
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void recoverStale() {
        Instant now = Instant.now();
        outboxRepository.recoverStale(now.minus(Duration.ofMinutes(properties.getOutbox().getStaleLockMinutes())), now);
    }

    private void fail(NotificationOutbox outbox, Exception ex) {
        int attempt = outbox.getAttemptCount() + 1;
        outbox.setAttemptCount(attempt);
        outbox.setLockedAt(null);
        outbox.setLastError(truncate(ex.getMessage()));
        if (attempt >= properties.getOutbox().getMaxAttempts()) {
            outbox.setStatus(NotificationOutboxStatus.DEAD);
            meterRegistry.counter("notification.outbox.processed", "status", "dead").increment();
            log.error("Notification outbox dead eventKey={} attemptCount={}", outbox.getEventKey(), attempt, ex);
        } else {
            outbox.setStatus(NotificationOutboxStatus.RETRY);
            outbox.setNextAttemptAt(Instant.now().plusSeconds(BACKOFF_SECONDS[Math.min(attempt - 1,
                    BACKOFF_SECONDS.length - 1)]));
            log.warn("Notification outbox retry eventKey={} attemptCount={}", outbox.getEventKey(), attempt);
        }
    }

    private String truncate(String value) {
        if (value == null) return "Unknown delivery error";
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
