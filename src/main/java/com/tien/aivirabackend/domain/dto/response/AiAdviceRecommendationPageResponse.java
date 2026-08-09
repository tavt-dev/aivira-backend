package com.tien.aivirabackend.domain.dto.response;

import java.util.List;

public record AiAdviceRecommendationPageResponse(
        List<AiAdviceRecommendationResponse> items,
        int page,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean hasNext) {}
