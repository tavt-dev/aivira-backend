package com.tien.aivirabackend.service.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.tien.aivirabackend.config.properties.GeminiProperties;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.AiAdviceErrorCode;

@Component
@ConditionalOnProperty(name = "ai-advice.provider", havingValue = "gemini", matchIfMissing = true)
public class GeminiAdvisorClient implements AiAdvisorClient {
    private static final Logger log = LoggerFactory.getLogger(GeminiAdvisorClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final GeminiProperties properties;

    public GeminiAdvisorClient(@Qualifier("geminiRestClient") RestClient restClient, ObjectMapper objectMapper,
            GeminiProperties properties) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public AiModelResult<AiSearchProfile> analyze(List<AiConversationTurn> history, String personalizationContext,
            String locale, String safetyIdentifier) {
        String language = locale != null && locale.startsWith("en") ? "English" : "Vietnamese";
        String instruction = """
                You analyze requests for a bookstore recommendation assistant.
                Extract only preferences supported by the conversation. Ask one concise clarification question when
                the request is too vague to produce useful recommendations. Use %s for the clarification question.
                Treat the latest user message as authoritative. Use older turns only to resolve references such as
                "those books"; never carry an older topic, price, or language into a new unrelated request.
                Prices are Vietnamese dong. Empty arrays mean no preference. Do not invent personal information.
                Aggregated preference summary, when enabled: %s
                """.formatted(language, personalizationContext == null ? "disabled" : personalizationContext);
        List<Map<String, Object>> contents = history.stream()
                .map(turn -> content("assistant".equals(turn.role()) ? "model" : "user", turn.content())).toList();
        return request(instruction, contents, profileSchema(), AiSearchProfile.class);
    }

    @Override
    public AiModelResult<AiAdviceDraft> explain(AiSearchProfile profile, List<AiBookCandidate> books, String locale,
            String safetyIdentifier) {
        String language = locale != null && locale.startsWith("en") ? "English" : "Vietnamese";
        try {
            String instruction = """
                    You are a warm, concise bookstore advisor. Reply in %s.
                    Recommend every supplied catalog book exactly once, using only its supplied productId and facts.
                    Give a distinct practical reason and short matched criteria for each book. Never invent books,
                    prices, ratings, authors, or claims not present in the catalog data.
                    """.formatted(language);
            String prompt = "User preference profile: %s\nCatalog candidates: %s"
                    .formatted(objectMapper.writeValueAsString(profile), objectMapper.writeValueAsString(books));
            return request(instruction, List.of(content("user", prompt)), adviceSchema(), AiAdviceDraft.class);
        } catch (JacksonException ex) {
            throw new AppException(AiAdviceErrorCode.INVALID_AI_RESPONSE, ex);
        }
    }

    private <T> AiModelResult<T> request(String instruction, List<Map<String, Object>> contents,
            Map<String, Object> schema, Class<T> type) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new AppException(AiAdviceErrorCode.AI_ADVISOR_UNAVAILABLE).addDetail("reason",
                    "GEMINI_API_KEY is missing");
        }
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", 0.2);
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("responseSchema", schema);
        Map<String, Object> body = Map.of("systemInstruction", Map.of("parts", List.of(Map.of("text", instruction))),
                "contents", contents, "generationConfig", generationConfig);

        long started = System.nanoTime();
        try {
            JsonNode response = restClient.post().uri("/v1beta/models/{model}:generateContent", properties.model())
                    .header("x-goog-api-key", properties.apiKey()).body(body).retrieve().body(JsonNode.class);
            long latencyMs = (System.nanoTime() - started) / 1_000_000;
            String text = extractText(response);
            T parsed = objectMapper.readValue(text, type);
            JsonNode usage = response.path("usageMetadata");
            log.info("AI request completed provider=gemini model={} latencyMs={}", properties.model(), latencyMs);
            return new AiModelResult<>(parsed, "gemini", properties.model(), usage.path("promptTokenCount").asInt(0),
                    usage.path("candidatesTokenCount").asInt(0), latencyMs);
        } catch (AppException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            HttpStatusCode status = ex.getStatusCode();
            log.warn("AI request failed provider=gemini model={} httpStatus={}", properties.model(), status.value());
            throw new AppException(AiAdviceErrorCode.AI_ADVISOR_UNAVAILABLE, ex).addDetail("providerStatus",
                    status.value());
        } catch (RestClientException ex) {
            log.warn("AI request failed provider=gemini model={} reason=transport", properties.model());
            throw new AppException(AiAdviceErrorCode.AI_ADVISOR_UNAVAILABLE, ex);
        } catch (JacksonException ex) {
            throw new AppException(AiAdviceErrorCode.INVALID_AI_RESPONSE, ex);
        }
    }

    private String extractText(JsonNode response) {
        if (response == null || !response.path("promptFeedback").path("blockReason").asText("").isEmpty()) {
            throw new AppException(AiAdviceErrorCode.AI_ADVISOR_UNAVAILABLE);
        }
        JsonNode candidate = response.path("candidates").path(0);
        String finishReason = candidate.path("finishReason").asText("");
        if (!finishReason.isEmpty() && !"STOP".equals(finishReason)) {
            throw new AppException(AiAdviceErrorCode.INVALID_AI_RESPONSE).addDetail("finishReason", finishReason);
        }
        StringBuilder result = new StringBuilder();
        for (JsonNode part : candidate.path("content").path("parts")) {
            if (!part.path("text").asText("").isEmpty())
                result.append(part.path("text").asText());
        }
        if (result.isEmpty())
            throw new AppException(AiAdviceErrorCode.INVALID_AI_RESPONSE);
        return result.toString();
    }

    private Map<String, Object> content(String role, String text) {
        return Map.of("role", role, "parts", List.of(Map.of("text", text == null ? "" : text)));
    }

    private Map<String, Object> profileSchema() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("needsClarification", Map.of("type", "boolean"));
        fields.put("clarificationQuestion", Map.of("type", "string"));
        fields.put("summary", Map.of("type", "string"));
        fields.put("searchTerms", stringArray());
        fields.put("categoryHints", stringArray());
        fields.put("authorHints", stringArray());
        fields.put("languages", stringArray());
        fields.put("minPrice", Map.of("type", "number", "nullable", true));
        fields.put("maxPrice", Map.of("type", "number", "nullable", true));
        fields.put("rankingPriorities", stringArray());
        return objectSchema(fields);
    }

    private Map<String, Object> adviceSchema() {
        Map<String, Object> reason = new LinkedHashMap<>();
        reason.put("productId", Map.of("type", "integer"));
        reason.put("reason", Map.of("type", "string"));
        reason.put("matchedCriteria", stringArray());
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("message", Map.of("type", "string"));
        fields.put("recommendations", Map.of("type", "array", "items", objectSchema(reason)));
        fields.put("suggestedPrompts", stringArray());
        return objectSchema(fields);
    }

    private Map<String, Object> stringArray() {
        return Map.of("type", "array", "items", Map.of("type", "string"));
    }

    private Map<String, Object> objectSchema(Map<String, Object> fields) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", fields);
        schema.put("required", new ArrayList<>(fields.keySet()));
        return schema;
    }
}
