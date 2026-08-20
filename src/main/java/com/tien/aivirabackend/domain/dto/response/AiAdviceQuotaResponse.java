package com.tien.aivirabackend.domain.dto.response;

import java.time.Instant;

public record AiAdviceQuotaResponse(int limit, int used, int remaining, Instant resetsAt) {
}
