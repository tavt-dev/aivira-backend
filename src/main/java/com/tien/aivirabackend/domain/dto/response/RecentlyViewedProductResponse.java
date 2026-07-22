package com.tien.aivirabackend.domain.dto.response;

import java.time.Instant;

import com.tien.aivirabackend.constant.ProductViewSource;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecentlyViewedProductResponse {
    private ProductResponse product;
    private Instant firstViewedAt;
    private Instant lastViewedAt;
    private long viewCount;
    private ProductViewSource lastSource;
}
