package com.tien.aivirabackend.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.tien.aivirabackend.constant.NotificationType;
import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.response.NotificationResponse;
import com.tien.aivirabackend.domain.dto.response.ReadAllNotificationsResponse;
import com.tien.aivirabackend.domain.dto.response.UnreadNotificationCountResponse;
import com.tien.aivirabackend.service.auth.CurrentUserService;
import com.tien.aivirabackend.service.notification.NotificationService;
import com.tien.aivirabackend.service.notification.NotificationSseRegistry;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {
    private final NotificationService notificationService;
    private final NotificationSseRegistry sseRegistry;
    private final CurrentUserService currentUserService;

    @GetMapping
    @Operation(summary = "List current user's notifications")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getInbox(
            @RequestParam(required = false) Boolean read, @RequestParam(required = false) NotificationType type,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getInbox(read, type, page, size)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadNotificationCountResponse>> unreadCount() {
        return ResponseEntity.ok(ApiResponse.success(notificationService.unreadCount()));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(@PathVariable Long notificationId) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.markRead(notificationId)));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<ReadAllNotificationsResponse>> markAllRead() {
        return ResponseEntity.ok(ApiResponse.success(notificationService.markAllRead()));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return sseRegistry.connect(currentUserService.getCurrentUserId());
    }
}
