package com.tien.aivirabackend.config;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.tien.aivirabackend.config.properties.AiAdviceProperties;
import com.tien.aivirabackend.config.properties.GeminiProperties;
import com.tien.aivirabackend.config.properties.OpenAiProperties;
import com.tien.aivirabackend.config.properties.RagProperties;
import com.tien.aivirabackend.service.ai.QdrantVectorStore;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AiAdviceConfigurationValidator {
    private final AiAdviceProperties advisor;
    private final GeminiProperties gemini;
    private final OpenAiProperties openAi;
    private final RagProperties rag;
    private final QdrantVectorStore qdrant;

    @PostConstruct
    void validate() {
        if (!advisor.failFast())
            return;
        String key = "openai".equalsIgnoreCase(advisor.provider()) ? openAi.apiKey() : gemini.apiKey();
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("AI advisor API key is required when AI_ADVICE_FAIL_FAST=true");
        }
        if (rag.enabled() && !qdrant.healthy()) {
            throw new IllegalStateException("Qdrant is required when RAG_ENABLED=true and AI_ADVICE_FAIL_FAST=true");
        }
    }
}
