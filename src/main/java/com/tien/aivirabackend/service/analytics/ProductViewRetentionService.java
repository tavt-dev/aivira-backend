package com.tien.aivirabackend.service.analytics;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tien.aivirabackend.config.properties.ProductViewProperties;
import com.tien.aivirabackend.repository.ProductViewEventRepository;
import com.tien.aivirabackend.repository.UserRecentlyViewedRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "PRODUCT-VIEW-RETENTION")
@Service
@RequiredArgsConstructor
public class ProductViewRetentionService {
    private final ProductViewEventRepository eventRepository;
    private final UserRecentlyViewedRepository recentlyViewedRepository;
    private final ProductViewProperties properties;

    @Scheduled(cron = "${analytics.product-view.cleanup-cron:0 30 3 * * *}")
    @Transactional
    public void cleanup() {
        Instant now = Instant.now();
        long anonymous = eventRepository.deleteByUserIsNullAndViewedAtBefore(
                now.minus(properties.getAnonymousRetentionDays(), ChronoUnit.DAYS));
        long events =
                eventRepository.deleteByViewedAtBefore(now.minus(properties.getEventRetentionDays(), ChronoUnit.DAYS));
        long recent = recentlyViewedRepository.deleteByLastViewedAtBefore(
                now.minus(properties.getRecentRetentionDays(), ChronoUnit.DAYS));
        int trimmed = recentlyViewedRepository.trimToMaximumPerUser(properties.getMaxRecentProducts());
        log.info(
                "product_view_cleanup anonymous={} events={} recent={} trimmed={}", anonymous, events, recent, trimmed);
    }
}
