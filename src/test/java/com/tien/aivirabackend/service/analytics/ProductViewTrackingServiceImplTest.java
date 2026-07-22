package com.tien.aivirabackend.service.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tien.aivirabackend.config.properties.ProductViewProperties;
import com.tien.aivirabackend.constant.ProductViewSource;
import com.tien.aivirabackend.domain.dto.request.ProductViewRequest;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.repository.ProductRepository;
import com.tien.aivirabackend.repository.ProductViewEventRepository;
import com.tien.aivirabackend.repository.UserRecentlyViewedRepository;
import com.tien.aivirabackend.repository.UserRepository;
import com.tien.aivirabackend.service.auth.CurrentUserService;
import com.tien.aivirabackend.service.catalog.ProductStatusPolicy;

@ExtendWith(MockitoExtension.class)
class ProductViewTrackingServiceImplTest {
    @Mock
    ProductRepository productRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    ProductViewEventRepository eventRepository;

    @Mock
    UserRecentlyViewedRepository recentlyViewedRepository;

    @Mock
    CurrentUserService currentUserService;

    @Mock
    ProductStatusPolicy productStatusPolicy;

    @Mock
    ProductViewRateLimiter rateLimiter;

    ProductViewTrackingServiceImpl service;

    @BeforeEach
    void setUp() {
        ProductViewProperties properties = new ProductViewProperties();
        properties.setHashPepper("test-pepper");
        service = new ProductViewTrackingServiceImpl(
                productRepository,
                userRepository,
                eventRepository,
                recentlyViewedRepository,
                currentUserService,
                productStatusPolicy,
                properties,
                rateLimiter);
    }

    @Test
    void record_shouldHashGuestIdentifiersAndInsertOneEvent() {
        Product product = Product.builder().id(7L).slug("clean-code").build();
        when(productRepository.findDetailedBySlug("clean-code")).thenReturn(Optional.of(product));
        when(productStatusPolicy.isPubliclyVisible(product)).thenReturn(true);
        when(currentUserService.findCurrentUserId()).thenReturn(Optional.empty());
        when(rateLimiter.allow(anyString())).thenReturn(true);
        when(eventRepository.insertIfAbsent(
                        eq(7L),
                        isNull(),
                        anyString(),
                        anyString(),
                        anyString(),
                        eq("SEARCH"),
                        eq("/products"),
                        any(),
                        anyLong()))
                .thenReturn(1);
        ProductViewRequest request = new ProductViewRequest();
        request.setAnonymousId("6d0e6e43-2293-4f61-89c8-31293e570fa0");
        request.setSessionId("798098c4-f881-4124-830e-7d25d1ab82bf");
        request.setSource(ProductViewSource.SEARCH);
        request.setReferrerPath("/products");

        var response = service.record("clean-code", request);

        assertThat(response.recorded()).isTrue();
        ArgumentCaptor<String> anonymousHash = ArgumentCaptor.forClass(String.class);
        verify(eventRepository)
                .insertIfAbsent(
                        eq(7L),
                        isNull(),
                        anonymousHash.capture(),
                        anyString(),
                        anyString(),
                        eq("SEARCH"),
                        eq("/products"),
                        any(),
                        anyLong());
        assertThat(anonymousHash.getValue()).hasSize(64).doesNotContain(request.getAnonymousId());
    }

    @Test
    void record_shouldReturnDeduplicatedWhenUniqueBucketAlreadyExists() {
        Product product = Product.builder().id(7L).slug("clean-code").build();
        when(productRepository.findDetailedBySlug("clean-code")).thenReturn(Optional.of(product));
        when(productStatusPolicy.isPubliclyVisible(product)).thenReturn(true);
        when(currentUserService.findCurrentUserId()).thenReturn(Optional.empty());
        when(rateLimiter.allow(anyString())).thenReturn(true);
        when(eventRepository.insertIfAbsent(
                        anyLong(),
                        isNull(),
                        anyString(),
                        isNull(),
                        anyString(),
                        anyString(),
                        isNull(),
                        any(),
                        anyLong()))
                .thenReturn(0);
        ProductViewRequest request = new ProductViewRequest();
        request.setAnonymousId("6d0e6e43-2293-4f61-89c8-31293e570fa0");

        var response = service.record("clean-code", request);

        assertThat(response.recorded()).isFalse();
        assertThat(response.deduplicated()).isTrue();
    }
}
