package com.tien.aivirabackend.domain.dto.response;

import java.time.Instant;

import com.tien.aivirabackend.constant.*;

public record RagIndexJobResponse(String id, RagJobType type, RagJobStatus status, int totalItems, int succeededItems,
        int failedItems, String error, Instant startedAt, Instant completedAt) {}

