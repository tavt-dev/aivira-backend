package com.tien.aivirabackend.service.notification;

import java.util.concurrent.atomic.AtomicLong;

import jakarta.annotation.PostConstruct;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tien.aivirabackend.config.properties.NotificationProperties;
import com.tien.aivirabackend.constant.NotificationOutboxStatus;
import com.tien.aivirabackend.repository.NotificationOutboxRepository;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationMetrics {
    private final NotificationOutboxRepository outboxRepository;
    private final NotificationProperties properties;
    private final MeterRegistry meterRegistry;
    private final AtomicLong pending = new AtomicLong();
    private final AtomicLong dead = new AtomicLong();

    @PostConstruct
    void register() {
        Gauge.builder("notification.outbox.pending", pending, AtomicLong::get).register(meterRegistry);
        Gauge.builder("notification.outbox.dead", dead, AtomicLong::get).register(meterRegistry);
    }

    @Scheduled(fixedDelay = 10_000)
    public void refresh() {
        if (!properties.isEnabled()) {
            return;
        }
        pending.set(outboxRepository.countByStatus(NotificationOutboxStatus.PENDING)
                + outboxRepository.countByStatus(NotificationOutboxStatus.RETRY));
        dead.set(outboxRepository.countByStatus(NotificationOutboxStatus.DEAD));
    }
}
