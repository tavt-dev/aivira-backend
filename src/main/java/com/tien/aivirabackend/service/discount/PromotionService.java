package com.tien.aivirabackend.service.discount;

import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.PromotionCreateRequest;
import com.tien.aivirabackend.domain.dto.request.PromotionUpdateRequest;
import com.tien.aivirabackend.domain.dto.response.PromotionResponse;

public interface PromotionService {
    PageResponse<PromotionResponse> getPromotions(int page, int size);

    PromotionResponse getPromotion(Long promotionId);

    PromotionResponse createPromotion(PromotionCreateRequest request);

    PromotionResponse updatePromotion(Long promotionId, PromotionUpdateRequest request);

    void deletePromotion(Long promotionId);
}
