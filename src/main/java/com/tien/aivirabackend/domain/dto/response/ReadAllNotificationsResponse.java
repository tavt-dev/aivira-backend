package com.tien.aivirabackend.domain.dto.response;

import java.time.Instant;

public record ReadAllNotificationsResponse(int updatedCount, Instant readAt) {
}
