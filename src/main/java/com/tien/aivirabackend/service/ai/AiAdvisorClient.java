package com.tien.aivirabackend.service.ai;

import java.util.List;

public interface AiAdvisorClient {
    AiModelResult<AiSearchProfile> analyze(List<AiConversationTurn> history, String personalizationContext,
            String locale, String safetyIdentifier);

    AiModelResult<AiAdviceDraft> explain(AiSearchProfile profile, List<AiBookCandidate> books, String locale,
            String safetyIdentifier);
}
