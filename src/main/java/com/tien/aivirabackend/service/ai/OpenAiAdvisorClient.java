package com.tien.aivirabackend.service.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.tien.aivirabackend.config.properties.OpenAiProperties;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.AiAdviceErrorCode;

@Component
@ConditionalOnProperty(name = "ai-advice.provider", havingValue = "openai")
public class OpenAiAdvisorClient implements AiAdvisorClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final OpenAiProperties properties;

    public OpenAiAdvisorClient(@Qualifier("openAiRestClient") RestClient restClient, ObjectMapper objectMapper,
            OpenAiProperties properties) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public AiModelResult<AiSearchProfile> analyze(List<AiConversationTurn> history, String personalizationContext,
            String locale, String safetyIdentifier) {
        String language = locale != null && locale.startsWith("en") ? "English" : "Vietnamese";
        String instructions = """
                You analyze requests for a bookstore recommendation assistant.
                Extract only preferences supported by the conversation. Ask one concise clarification question when
                the request is too vague to produce useful recommendations. Use %s for the clarification question.
                Prices are Vietnamese dong. Empty arrays mean no preference. Do not invent personal information.
                Personalization summary, when enabled: %s
                """.formatted(language, personalizationContext == null ? "disabled" : personalizationContext);
        List<Map<String, Object>> input = new ArrayList<>();
        input.add(message("developer", instructions));
        for (AiConversationTurn turn : history) {
            input.add(message(turn.role(), turn.content()));
        }
        return request(input, profileSchema(), AiSearchProfile.class, safetyIdentifier);
    }

    @Override
    public AiModelResult<AiAdviceDraft> explain(AiSearchProfile profile, List<AiBookCandidate> books, String locale,
            String safetyIdentifier) {
        String language = locale != null && locale.startsWith("en") ? "English" : "Vietnamese";
        try {
            String instructions = """
                    You are a warm, concise bookstore advisor. Reply in %s.
                    Recommend every supplied catalog book exactly once, using only its supplied productId and facts.
                    Give a distinct practical reason and short matched criteria for each book. Never invent books,
                    prices, ratings, authors, or claims not present in the catalog data.
                    User preference profile: %s
                    Catalog page: %s
                    """.formatted(language, objectMapper.writeValueAsString(profile),
                    objectMapper.writeValueAsString(books));
            return request(List.of(message("developer", instructions)), adviceSchema(), AiAdviceDraft.class,
                    safetyIdentifier);
        } catch (JacksonException ex) {
            throw new AppException(AiAdviceErrorCode.INVALID_AI_RESPONSE, ex);
        }
    }

    private <T> AiModelResult<T> request(List<Map<String, Object>> input, Map<String, Object> schema, Class<T> type,
            String safetyIdentifier) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new AppException(AiAdviceErrorCode.AI_ADVISOR_UNAVAILABLE).addDetail("reason",
                    "OPENAI_API_KEY is missing");
        }

        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", type == AiSearchProfile.class ? "book_search_profile" : "book_advice");
        format.put("strict", true);
        format.put("schema", schema);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.model());
        body.put("reasoning", Map.of("effort", "none", "context", "current_turn"));
        body.put("store", false);
        body.put("safety_identifier", safetyIdentifier);
        body.put("input", input);
        body.put("text", Map.of("verbosity", "low", "format", format));

        long started = System.nanoTime();
        try {
            JsonNode response = restClient.post().uri("/responses").body(body).retrieve().body(JsonNode.class);
            long latencyMs = (System.nanoTime() - started) / 1_000_000;
            if (response == null) {
                throw new AppException(AiAdviceErrorCode.INVALID_AI_RESPONSE);
            }
            String outputText = extractOutputText(response);
            T parsed = objectMapper.readValue(outputText, type);
            JsonNode usage = response.path("usage");
            return new AiModelResult<>(parsed, "openai", response.path("model").asText(properties.model()),
                    usage.path("input_tokens").asInt(0), usage.path("output_tokens").asInt(0), latencyMs);
        } catch (AppException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new AppException(AiAdviceErrorCode.AI_ADVISOR_UNAVAILABLE, ex);
        } catch (JacksonException ex) {
            throw new AppException(AiAdviceErrorCode.INVALID_AI_RESPONSE, ex);
        }
    }

    private String extractOutputText(JsonNode response) {
        StringBuilder text = new StringBuilder();
        for (JsonNode output : response.path("output")) {
            for (JsonNode content : output.path("content")) {
                if ("refusal".equals(content.path("type").asText())) {
                    throw new AppException(AiAdviceErrorCode.AI_ADVISOR_UNAVAILABLE);
                }
                if ("output_text".equals(content.path("type").asText())) {
                    text.append(content.path("text").asText());
                }
            }
        }
        if (text.isEmpty()) {
            throw new AppException(AiAdviceErrorCode.INVALID_AI_RESPONSE);
        }
        return text.toString();
    }

    private Map<String, Object> message(String role, String content) {
        return Map.of("role", role, "content", content == null ? "" : content);
    }

    private Map<String, Object> profileSchema() {
        Map<String, Object> propertiesMap = new LinkedHashMap<>();
        propertiesMap.put("needsClarification", Map.of("type", "boolean"));
        propertiesMap.put("clarificationQuestion", Map.of("type", "string"));
        propertiesMap.put("summary", Map.of("type", "string"));
        propertiesMap.put("searchTerms", stringArray());
        propertiesMap.put("categoryHints", stringArray());
        propertiesMap.put("authorHints", stringArray());
        propertiesMap.put("languages", stringArray());
        propertiesMap.put("minPrice", Map.of("type", List.of("number", "null")));
        propertiesMap.put("maxPrice", Map.of("type", List.of("number", "null")));
        propertiesMap.put("rankingPriorities", stringArray());
        return objectSchema(propertiesMap);
    }

    private Map<String, Object> adviceSchema() {
        Map<String, Object> recommendationProperties = new LinkedHashMap<>();
        recommendationProperties.put("productId", Map.of("type", "integer"));
        recommendationProperties.put("reason", Map.of("type", "string"));
        recommendationProperties.put("matchedCriteria", stringArray());

        Map<String, Object> propertiesMap = new LinkedHashMap<>();
        propertiesMap.put("message", Map.of("type", "string"));
        propertiesMap.put("recommendations", Map.of("type", "array", "items", objectSchema(recommendationProperties)));
        propertiesMap.put("suggestedPrompts", stringArray());
        return objectSchema(propertiesMap);
    }

    private Map<String, Object> stringArray() {
        return Map.of("type", "array", "items", Map.of("type", "string"));
    }

    private Map<String, Object> objectSchema(Map<String, Object> propertiesMap) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", propertiesMap);
        schema.put("required", new ArrayList<>(propertiesMap.keySet()));
        schema.put("additionalProperties", false);
        return schema;
    }
}
