package com.tien.aivirabackend.service.analytics;

import com.tien.aivirabackend.domain.dto.request.ProductViewRequest;
import com.tien.aivirabackend.domain.dto.response.ProductViewRecordResponse;

public interface ProductViewTrackingService {
    ProductViewRecordResponse record(String productSlug, ProductViewRequest request);
}
