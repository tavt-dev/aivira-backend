package com.tien.aivirabackend.config;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.tien.aivirabackend.config.properties.AiAdviceProperties;
import com.tien.aivirabackend.config.properties.GeminiProperties;
import com.tien.aivirabackend.config.properties.OpenAiProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AiAdviceConfigurationValidator {
    private final AiAdviceProperties advisor;
    private final GeminiProperties gemini;
    private final OpenAiProperties openAi;

    @PostConstruct
    void validate() {
        if (!advisor.failFast())
            return;
        String key = "openai".equalsIgnoreCase(advisor.provider()) ? openAi.apiKey() : gemini.apiKey();
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("AI advisor API key is required when AI_ADVICE_FAIL_FAST=true");
        }
    }
}
