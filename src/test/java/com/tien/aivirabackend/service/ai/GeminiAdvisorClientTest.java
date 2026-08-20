package com.tien.aivirabackend.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withTooManyRequests;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.ObjectMapper;
import com.tien.aivirabackend.config.properties.GeminiProperties;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.AiAdviceErrorCode;

class GeminiAdvisorClientTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void analyzesWithStructuredOutputAndUsageMetadata() throws Exception {
        Fixture fixture = fixture("key");
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("needsClarification", false);
        profile.put("clarificationQuestion", "");
        profile.put("summary", "Programming for beginners");
        profile.put("searchTerms", List.of("programming"));
        profile.put("categoryHints", List.of("technology"));
        profile.put("authorHints", List.of());
        profile.put("languages", List.of("Vietnamese"));
        profile.put("minPrice", null);
        profile.put("maxPrice", 300000);
        profile.put("rankingPriorities", List.of("beginner friendly"));
        String response = objectMapper.writeValueAsString(Map.of("candidates",
                List.of(Map.of("finishReason", "STOP", "content",
                        Map.of("parts", List.of(Map.of("text", objectMapper.writeValueAsString(profile)))))),
                "usageMetadata", Map.of("promptTokenCount", 25, "candidatesTokenCount", 14)));

        fixture.server.expect(requestTo("http://gemini.test/v1beta/models/gemini-2.5-flash:generateContent"))
                .andExpect(header("x-goog-api-key", "key"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("systemInstruction")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("responseMimeType")))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        AiModelResult<AiSearchProfile> result = fixture.client
                .analyze(List.of(new AiConversationTurn("user", "Tôi muốn học lập trình")), null, "vi", "safe-user");

        assertThat(result.provider()).isEqualTo("gemini");
        assertThat(result.model()).isEqualTo("gemini-2.5-flash");
        assertThat(result.inputTokens()).isEqualTo(25);
        assertThat(result.outputTokens()).isEqualTo(14);
        assertThat(result.value().searchTerms()).containsExactly("programming");
        fixture.server.verify();
    }

    @Test
    void mapsRateLimitToAdvisorUnavailable() {
        Fixture fixture = fixture("key");
        fixture.server.expect(requestTo("http://gemini.test/v1beta/models/gemini-2.5-flash:generateContent"))
                .andRespond(withTooManyRequests());

        assertThatThrownBy(
                () -> fixture.client.analyze(List.of(new AiConversationTurn("user", "books")), null, "en", "safe-user"))
                        .isInstanceOf(AppException.class)
                        .satisfies(error -> assertThat(((AppException) error).getErrorCode())
                                .isEqualTo(AiAdviceErrorCode.AI_ADVISOR_UNAVAILABLE));
    }

    @Test
    void rejectsSafetyBlockedResponse() {
        Fixture fixture = fixture("key");
        fixture.server.expect(requestTo("http://gemini.test/v1beta/models/gemini-2.5-flash:generateContent"))
                .andRespond(
                        withSuccess("{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(
                () -> fixture.client.analyze(List.of(new AiConversationTurn("user", "books")), null, "en", "safe-user"))
                        .isInstanceOf(AppException.class)
                        .satisfies(error -> assertThat(((AppException) error).getErrorCode())
                                .isEqualTo(AiAdviceErrorCode.AI_ADVISOR_UNAVAILABLE));
    }

    @Test
    void rejectsMissingApiKeyWithoutCallingProvider() {
        Fixture fixture = fixture("");
        assertThatThrownBy(
                () -> fixture.client.analyze(List.of(new AiConversationTurn("user", "books")), null, "en", "safe-user"))
                        .isInstanceOf(AppException.class)
                        .satisfies(error -> assertThat(((AppException) error).getErrorCode())
                                .isEqualTo(AiAdviceErrorCode.AI_ADVISOR_UNAVAILABLE));
    }

    private Fixture fixture(String key) {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://gemini.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiProperties properties = new GeminiProperties(key, "http://gemini.test", "gemini-2.5-flash",
                Duration.ofSeconds(1), Duration.ofSeconds(3));
        return new Fixture(new GeminiAdvisorClient(builder.build(), objectMapper, properties), server);
    }

    private record Fixture(GeminiAdvisorClient client, MockRestServiceServer server) {
    }
}
