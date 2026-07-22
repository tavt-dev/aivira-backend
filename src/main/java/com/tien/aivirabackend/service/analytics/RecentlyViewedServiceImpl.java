package com.tien.aivirabackend.service.analytics;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tien.aivirabackend.config.properties.ProductViewProperties;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.ClaimAnonymousHistoryRequest;
import com.tien.aivirabackend.domain.dto.response.ClaimAnonymousHistoryResponse;
import com.tien.aivirabackend.domain.dto.response.RecentlyViewedProductResponse;
import com.tien.aivirabackend.domain.entity.analytics.ProductViewEvent;
import com.tien.aivirabackend.domain.entity.analytics.UserRecentlyViewed;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.mapper.ProductViewMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.AnalyticsErrorCode;
import com.tien.aivirabackend.repository.ProductViewEventRepository;
import com.tien.aivirabackend.repository.UserRecentlyViewedRepository;
import com.tien.aivirabackend.service.auth.CurrentUserService;
import com.tien.aivirabackend.util.PageRequestUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecentlyViewedServiceImpl implements RecentlyViewedService {
    private final UserRecentlyViewedRepository recentlyViewedRepository;
    private final ProductViewEventRepository eventRepository;
    private final CurrentUserService currentUserService;
    private final ProductViewTrackingServiceImpl trackingService;
    private final ProductViewMapper mapper;
    private final ProductViewProperties properties;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RecentlyViewedProductResponse> getMine(int page, int size) {
        String userId = currentUserService.getCurrentUserId();
        var result = recentlyViewedRepository
                .findVisibleByUserId(
                        userId,
                        PageRequestUtils.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "lastViewedAt")))
                .map(mapper::toResponse);
        return PageResponse.from(result);
    }

    @Override
    @Transactional
    public void remove(Long productId) {
        if (recentlyViewedRepository.deleteByUserIdAndProductId(currentUserService.getCurrentUserId(), productId)
                == 0) {
            throw new AppException(AnalyticsErrorCode.RECENTLY_VIEWED_NOT_FOUND);
        }
    }

    @Override
    @Transactional
    public void clear() {
        String userId = currentUserService.getCurrentUserId();
        recentlyViewedRepository.deleteByUserId(userId);
        eventRepository.anonymizeByUserId(userId);
    }

    @Override
    @Transactional
    public ClaimAnonymousHistoryResponse claim(ClaimAnonymousHistoryRequest request) {
        String anonymousHash = validateAndHash(request.getAnonymousId());
        Instant cutoff = Instant.now().minus(properties.getAnonymousRetentionDays(), ChronoUnit.DAYS);
        List<ProductViewEvent> events =
                eventRepository.findByAnonymousIdHashAndUserIsNullAndViewedAtGreaterThanEqual(anonymousHash, cutoff);
        if (events.isEmpty()) return new ClaimAnonymousHistoryResponse(0);

        User user = currentUserService.getCurrentUser();
        Map<Long, List<ProductViewEvent>> byProduct = events.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        event -> event.getProduct().getId(), LinkedHashMap::new, java.util.stream.Collectors.toList()));
        byProduct.values().forEach(productEvents -> merge(user, productEvents));
        events.forEach(event -> event.setUser(user));
        eventRepository.saveAll(events);
        return new ClaimAnonymousHistoryResponse(byProduct.size());
    }

    private String validateAndHash(String anonymousId) {
        try {
            java.util.UUID.fromString(anonymousId);
        } catch (IllegalArgumentException exception) {
            throw new AppException(AnalyticsErrorCode.INVALID_ANONYMOUS_ID);
        }
        return trackingService.hash("ANON_ID:" + anonymousId);
    }

    private void merge(User user, List<ProductViewEvent> events) {
        events.sort(Comparator.comparing(ProductViewEvent::getViewedAt));
        ProductViewEvent first = events.getFirst();
        ProductViewEvent last = events.getLast();
        UserRecentlyViewed recent = recentlyViewedRepository
                .findByUserIdAndProductId(user.getId(), first.getProduct().getId())
                .orElseGet(() -> UserRecentlyViewed.builder()
                        .user(user)
                        .product(first.getProduct())
                        .firstViewedAt(first.getViewedAt())
                        .lastViewedAt(last.getViewedAt())
                        .viewCount(0L)
                        .lastSource(last.getSource())
                        .build());
        if (first.getViewedAt().isBefore(recent.getFirstViewedAt())) recent.setFirstViewedAt(first.getViewedAt());
        if (last.getViewedAt().isAfter(recent.getLastViewedAt())) {
            recent.setLastViewedAt(last.getViewedAt());
            recent.setLastSource(last.getSource());
        }
        recent.setViewCount(recent.getViewCount() + events.size());
        recentlyViewedRepository.save(recent);
    }
}
