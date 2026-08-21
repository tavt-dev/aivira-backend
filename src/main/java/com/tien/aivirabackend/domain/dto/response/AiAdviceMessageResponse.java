package com.tien.aivirabackend.domain.dto.response;

import java.time.Instant;
import java.util.List;

import com.tien.aivirabackend.constant.AiAdviceResponseStatus;
import com.tien.aivirabackend.constant.AiAdviceRole;
import com.tien.aivirabackend.constant.RetrievalMode;

public record AiAdviceMessageResponse(Long id, AiAdviceRole role, String content, AiAdviceResponseStatus status,
        Instant createdAt, List<String> suggestedPrompts, AiAdviceRecommendationPageResponse recommendations,
        AiAdviceQuotaResponse quota, RetrievalMode retrievalMode, String embeddingProvider, String embeddingModel) {
}
