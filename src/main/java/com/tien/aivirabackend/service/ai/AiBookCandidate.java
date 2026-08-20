package com.tien.aivirabackend.service.ai;

import java.math.BigDecimal;

public record AiBookCandidate(Long productId, String title, String author, String category, String description,
        String language, BigDecimal price, BigDecimal rating, Integer soldCount) {
}
