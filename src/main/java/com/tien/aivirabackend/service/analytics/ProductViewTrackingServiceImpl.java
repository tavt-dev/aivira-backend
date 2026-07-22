package com.tien.aivirabackend.service.analytics;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.tien.aivirabackend.config.properties.ProductViewProperties;
import com.tien.aivirabackend.constant.ProductViewSource;
import com.tien.aivirabackend.domain.dto.request.ProductViewRequest;
import com.tien.aivirabackend.domain.dto.response.ProductViewRecordResponse;
import com.tien.aivirabackend.domain.entity.analytics.UserRecentlyViewed;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.AnalyticsErrorCode;
import com.tien.aivirabackend.repository.ProductRepository;
import com.tien.aivirabackend.repository.ProductViewEventRepository;
import com.tien.aivirabackend.repository.UserRecentlyViewedRepository;
import com.tien.aivirabackend.repository.UserRepository;
import com.tien.aivirabackend.service.auth.CurrentUserService;
import com.tien.aivirabackend.service.catalog.ProductStatusPolicy;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductViewTrackingServiceImpl implements ProductViewTrackingService {
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductViewEventRepository eventRepository;
    private final UserRecentlyViewedRepository recentlyViewedRepository;
    private final CurrentUserService currentUserService;
    private final ProductStatusPolicy productStatusPolicy;
    private final ProductViewProperties properties;
    private final ProductViewRateLimiter rateLimiter;

    @Override
    @Transactional
    public ProductViewRecordResponse record(String productSlug, ProductViewRequest request) {
        if (!properties.isEnabled()) return new ProductViewRecordResponse(false, false);

        Product product = productRepository
                .findDetailedBySlug(productSlug)
                .filter(productStatusPolicy::isPubliclyVisible)
                .orElseThrow(() -> new AppException(AnalyticsErrorCode.PRODUCT_NOT_VIEWABLE));
        String userId = currentUserService.findCurrentUserId().orElse(null);
        String anonymousHash = validateAndHashAnonymous(request.getAnonymousId(), userId == null);
        String sessionHash = validateAndHashOptional(request.getSessionId(), AnalyticsErrorCode.INVALID_SESSION_ID);
        ProductViewSource source = request.getSource() == null ? ProductViewSource.DIRECT : request.getSource();
        String referrerPath = validateReferrer(request.getReferrerPath());
        String viewerKey = hash((userId == null ? "ANON:" + anonymousHash : "USER:" + userId));
        if (!rateLimiter.allow(sessionHash == null ? viewerKey : sessionHash)) {
            throw new AppException(AnalyticsErrorCode.VIEW_RATE_LIMIT_EXCEEDED);
        }

        Instant now = Instant.now();
        long bucketSeconds = Math.max(1, properties.getDeduplicationMinutes()) * 60L;
        long bucket = now.getEpochSecond() / bucketSeconds;
        int inserted = eventRepository.insertIfAbsent(
                product.getId(),
                userId,
                anonymousHash,
                sessionHash,
                viewerKey,
                source.name(),
                referrerPath,
                now,
                bucket);

        if (userId != null) upsertRecent(userRepository.getReferenceById(userId), product, source, now, inserted == 1);
        return new ProductViewRecordResponse(inserted == 1, inserted == 0);
    }

    private void upsertRecent(User user, Product product, ProductViewSource source, Instant now, boolean increment) {
        UserRecentlyViewed recent = recentlyViewedRepository
                .findByUserIdAndProductId(user.getId(), product.getId())
                .orElseGet(() -> UserRecentlyViewed.builder()
                        .user(user)
                        .product(product)
                        .firstViewedAt(now)
                        .lastViewedAt(now)
                        .viewCount(increment ? 0L : 1L)
                        .lastSource(source)
                        .build());
        recent.setLastViewedAt(now);
        recent.setLastSource(source);
        if (increment) recent.setViewCount(recent.getViewCount() + 1);
        recentlyViewedRepository.save(recent);
    }

    String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of()
                    .formatHex(
                            digest.digest((properties.getHashPepper() + ":" + value).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String validateAndHashAnonymous(String value, boolean required) {
        if (!StringUtils.hasText(value)) {
            if (required) throw new AppException(AnalyticsErrorCode.VIEWER_ID_REQUIRED);
            return null;
        }
        validateUuid(value, AnalyticsErrorCode.INVALID_ANONYMOUS_ID);
        return hash("ANON_ID:" + value);
    }

    private String validateAndHashOptional(String value, AnalyticsErrorCode errorCode) {
        if (!StringUtils.hasText(value)) return null;
        validateUuid(value, errorCode);
        return hash("SESSION:" + value);
    }

    private void validateUuid(String value, AnalyticsErrorCode errorCode) {
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new AppException(errorCode);
        }
    }

    private String validateReferrer(String value) {
        if (!StringUtils.hasText(value)) return null;
        if (!value.startsWith("/") || value.startsWith("//")) {
            throw new AppException(AnalyticsErrorCode.INVALID_REFERRER_PATH);
        }
        return value;
    }
}
