package com.tien.aivirabackend.domain.dto.response;

public record RagIndexStatusResponse(boolean enabled, boolean qdrantHealthy, String collection, String provider,
        String model, int dimension, long indexed, long pending, long failed) {}

