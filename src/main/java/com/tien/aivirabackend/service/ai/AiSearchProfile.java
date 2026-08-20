package com.tien.aivirabackend.service.ai;

import java.math.BigDecimal;
import java.util.List;

public record AiSearchProfile(boolean needsClarification, String clarificationQuestion, String summary,
        List<String> searchTerms, List<String> categoryHints, List<String> authorHints, List<String> languages,
        BigDecimal minPrice, BigDecimal maxPrice, List<String> rankingPriorities) {
}
