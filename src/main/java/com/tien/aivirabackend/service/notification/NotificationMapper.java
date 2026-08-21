package com.tien.aivirabackend.service.notification;

import java.util.Collections;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.tien.aivirabackend.domain.dto.response.NotificationResponse;
import com.tien.aivirabackend.domain.entity.notification.Notification;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationMapper {
    private final ObjectMapper objectMapper;

    public NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder().id(notification.getId()).type(notification.getType())
                .title(notification.getTitle()).message(notification.getMessage())
                .resourceType(notification.getResourceType()).resourceId(notification.getResourceId())
                .actionUrl(notification.getActionUrl()).payload(readPayload(notification.getPayload()))
                .read(notification.getReadAt() != null).readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt()).build();
    }

    public String writePayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Cannot serialize notification payload", ex);
        }
    }

    private Map<String, Object> readPayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(payload, new TypeReference<>() {});
        } catch (JacksonException ex) {
            return Collections.emptyMap();
        }
    }
}
