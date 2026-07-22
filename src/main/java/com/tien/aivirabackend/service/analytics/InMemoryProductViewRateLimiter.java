package com.tien.aivirabackend.service.analytics;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import com.tien.aivirabackend.config.properties.ProductViewProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InMemoryProductViewRateLimiter implements ProductViewRateLimiter {
    private final ProductViewProperties properties;
    private final Clock clock = Clock.systemUTC();
    private final ConcurrentHashMap<String, WindowCounter> minuteCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, WindowCounter> hourCounters = new ConcurrentHashMap<>();

    @Override
    public boolean allow(String viewerKey) {
        Instant now = clock.instant();
        return increment(minuteCounters, viewerKey, now.getEpochSecond() / 60, properties.getMinuteRateLimit())
                && increment(hourCounters, viewerKey, now.getEpochSecond() / 3600, properties.getHourlyRateLimit());
    }

    private boolean increment(ConcurrentHashMap<String, WindowCounter> counters, String key, long window, int maximum) {
        WindowCounter counter = counters.compute(key, (ignored, existing) -> {
            if (existing == null || existing.window != window) return new WindowCounter(window);
            return existing;
        });
        return counter.count.incrementAndGet() <= maximum;
    }

    private static final class WindowCounter {
        private final long window;
        private final AtomicInteger count = new AtomicInteger();

        private WindowCounter(long window) {
            this.window = window;
        }
    }
}
