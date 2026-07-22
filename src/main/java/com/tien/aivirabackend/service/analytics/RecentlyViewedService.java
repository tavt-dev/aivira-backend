package com.tien.aivirabackend.service.analytics;

import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.ClaimAnonymousHistoryRequest;
import com.tien.aivirabackend.domain.dto.response.ClaimAnonymousHistoryResponse;
import com.tien.aivirabackend.domain.dto.response.RecentlyViewedProductResponse;

public interface RecentlyViewedService {
    PageResponse<RecentlyViewedProductResponse> getMine(int page, int size);

    void remove(Long productId);

    void clear();

    ClaimAnonymousHistoryResponse claim(ClaimAnonymousHistoryRequest request);
}
