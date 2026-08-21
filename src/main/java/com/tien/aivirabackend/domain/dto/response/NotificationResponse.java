package com.tien.aivirabackend.domain.dto.response;

import java.time.Instant;
import java.util.Map;

import com.tien.aivirabackend.constant.NotificationResourceType;
import com.tien.aivirabackend.constant.NotificationType;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    Long id;
    NotificationType type;
    String title;
    String message;
    NotificationResourceType resourceType;
    String resourceId;
    String actionUrl;
    Map<String, Object> payload;
    boolean read;
    Instant readAt;
    Instant createdAt;
}
