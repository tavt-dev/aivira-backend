package com.tien.aivirabackend.service.ai;

public record AiModelResult<T>(T value, String provider, String model, int inputTokens, int outputTokens,
        long latencyMs) {
}
