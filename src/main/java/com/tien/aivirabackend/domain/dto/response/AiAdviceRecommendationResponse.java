package com.tien.aivirabackend.domain.dto.response;

import java.util.List;

public record AiAdviceRecommendationResponse(Long id, int rank, ProductResponse product, String reason,
        List<String> matchedCriteria, Double semanticScore, Double lexicalScore, Double finalScore) {
}
