package com.tien.aivirabackend.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.domain.dto.request.*;
import com.tien.aivirabackend.domain.dto.response.*;
import com.tien.aivirabackend.service.ai.AiAdviceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ai-advice")
@RequiredArgsConstructor
@Tag(name = "AI Book Advice")
public class AiAdviceController {
    private final AiAdviceService service;

    @PostMapping("/sessions")
    @Operation(summary = "Create an authenticated AI book-advice session")
    public ResponseEntity<ApiResponse<AiAdviceSessionResponse>> createSession(
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            @Valid @RequestBody AiAdviceSessionCreateRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("AI advice session created", service.createSession(request, guestId)));
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<AiAdviceSessionResponse>> getSession(@PathVariable String sessionId,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId) {
        return ResponseEntity.ok(ApiResponse.success(service.getSession(sessionId, guestId)));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<ApiResponse<AiAdviceMessageResponse>> sendMessage(@PathVariable String sessionId,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            @Valid @RequestBody AiAdviceMessageRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("AI advice completed", service.sendMessage(sessionId, request, guestId)));
    }

    @GetMapping("/sessions/{sessionId}/messages/{messageId}/recommendations")
    public ResponseEntity<ApiResponse<AiAdviceRecommendationPageResponse>> getRecommendations(
            @PathVariable String sessionId, @PathVariable Long messageId,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(ApiResponse.success(service.getRecommendations(sessionId, messageId, page, guestId)));
    }

    @PatchMapping("/sessions/{sessionId}/preferences")
    public ResponseEntity<ApiResponse<AiAdviceSessionResponse>> updatePreferences(@PathVariable String sessionId,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            @Valid @RequestBody AiAdvicePreferencesRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.updatePreferences(sessionId, request, guestId)));
    }

    @PostMapping("/sessions/{sessionId}/events")
    public ResponseEntity<ApiResponse<Void>> recordEvent(@PathVariable String sessionId,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId,
            @Valid @RequestBody AiAdviceEventRequest request) {
        service.recordEvent(sessionId, request, guestId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/quota")
    public ResponseEntity<ApiResponse<AiAdviceQuotaResponse>> getQuota() {
        return ResponseEntity.ok(ApiResponse.success(service.getQuota()));
    }
}
