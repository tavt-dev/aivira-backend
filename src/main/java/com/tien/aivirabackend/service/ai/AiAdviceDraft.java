package com.tien.aivirabackend.service.ai;

import java.util.List;

public record AiAdviceDraft(String message, List<BookReason> recommendations, List<String> suggestedPrompts) {
    public record BookReason(Long productId, String reason, List<String> matchedCriteria) {
    }
}
