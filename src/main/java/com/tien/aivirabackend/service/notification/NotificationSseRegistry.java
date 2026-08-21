package com.tien.aivirabackend.service.notification;

import java.io.IOException;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.tien.aivirabackend.config.properties.NotificationProperties;
import com.tien.aivirabackend.domain.dto.response.NotificationResponse;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationSseRegistry {
    private final NotificationProperties properties;
    private final MeterRegistry meterRegistry;
    private final Map<String, Deque<SseEmitter>> connections = new ConcurrentHashMap<>();
    private final AtomicInteger connectionCount = new AtomicInteger();

    @PostConstruct
    void registerMetrics() {
        meterRegistry.gauge("notification.sse.connections", connectionCount);
    }

    public SseEmitter connect(String userId) {
        Deque<SseEmitter> userConnections =
                connections.computeIfAbsent(userId, ignored -> new ConcurrentLinkedDeque<>());
        while (userConnections.size() >= properties.getSse().getMaxConnectionsPerUser()) {
            SseEmitter oldest = userConnections.pollFirst();
            if (oldest != null) {
                connectionCount.decrementAndGet();
                oldest.complete();
            }
        }
        SseEmitter emitter = new SseEmitter(properties.getSse().getTimeoutMs());
        userConnections.addLast(emitter);
        connectionCount.incrementAndGet();
        Runnable remove = () -> remove(userId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(error -> remove.run());
        try {
            emitter.send(SseEmitter.event().name("connected").data(Map.of("connected", true)));
        } catch (IOException ex) {
            remove.run();
            emitter.completeWithError(ex);
        }
        return emitter;
    }

    public void publish(String userId, NotificationResponse notification) {
        Deque<SseEmitter> emitters = connections.get(userId);
        if (emitters == null) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().id(String.valueOf(notification.getId()))
                        .name("notification.created").data(notification));
            } catch (Exception ex) {
                remove(userId, emitter);
                emitter.complete();
            }
        }
    }

    @Scheduled(fixedDelayString = "${notification.sse.heartbeat-ms:25000}")
    public void heartbeat() {
        connections.forEach((userId, emitters) -> emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("heartbeat").comment("keep-alive"));
            } catch (Exception ex) {
                remove(userId, emitter);
                emitter.complete();
            }
        }));
    }

    private void remove(String userId, SseEmitter emitter) {
        Deque<SseEmitter> emitters = connections.get(userId);
        if (emitters != null) {
            if (emitters.remove(emitter)) {
                connectionCount.decrementAndGet();
            }
            if (emitters.isEmpty()) {
                connections.remove(userId, emitters);
            }
        }
    }
}
