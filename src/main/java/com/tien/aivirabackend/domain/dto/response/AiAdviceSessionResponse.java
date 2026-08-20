package com.tien.aivirabackend.domain.dto.response;

import java.time.Instant;
import java.util.List;

public record AiAdviceSessionResponse(String id, String locale, boolean personalizationEnabled, Instant expiresAt,
        List<AiAdviceMessageResponse> messages, AiAdviceQuotaResponse quota) {
}
