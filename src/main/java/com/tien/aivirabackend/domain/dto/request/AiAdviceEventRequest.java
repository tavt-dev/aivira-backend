package com.tien.aivirabackend.domain.dto.request;

import jakarta.validation.constraints.NotNull;

import com.tien.aivirabackend.constant.AiAdviceEventType;

public record AiAdviceEventRequest(@NotNull AiAdviceEventType eventType, Long messageId, Long recommendationId) {
}
