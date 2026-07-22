package com.tien.aivirabackend.domain.mapper;

import org.springframework.stereotype.Component;

import com.tien.aivirabackend.domain.dto.response.RecentlyViewedProductResponse;
import com.tien.aivirabackend.domain.entity.analytics.UserRecentlyViewed;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProductViewMapper {
    private final ProductMapper productMapper;

    public RecentlyViewedProductResponse toResponse(UserRecentlyViewed recent) {
        return RecentlyViewedProductResponse.builder()
                .product(productMapper.toResponse(recent.getProduct()))
                .firstViewedAt(recent.getFirstViewedAt())
                .lastViewedAt(recent.getLastViewedAt())
                .viewCount(recent.getViewCount())
                .lastSource(recent.getLastSource())
                .build();
    }
}
