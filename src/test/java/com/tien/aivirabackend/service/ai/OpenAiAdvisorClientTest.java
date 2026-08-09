package com.tien.aivirabackend.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.ObjectMapper;
import com.tien.aivirabackend.config.properties.OpenAiProperties;

class OpenAiAdvisorClientTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void analyzesWithLunaStructuredResponsesAndNoStorage() throws Exception {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://openai.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiAdvisorClient client = new OpenAiAdvisorClient(builder.build(), objectMapper, properties("key"));

        Map<String, Object> structuredValue = new LinkedHashMap<>();
        structuredValue.put("needsClarification", false);
        structuredValue.put("clarificationQuestion", "");
        structuredValue.put("summary", "Programming for beginners");
        structuredValue.put("searchTerms", List.of("programming"));
        structuredValue.put("categoryHints", List.of("technology"));
        structuredValue.put("authorHints", List.of());
        structuredValue.put("languages", List.of("Vietnamese"));
        structuredValue.put("minPrice", null);
        structuredValue.put("maxPrice", 300000);
        structuredValue.put("rankingPriorities", List.of("beginner friendly"));
        String structured = objectMapper.writeValueAsString(structuredValue);
        String response = objectMapper.writeValueAsString(Map.of(
                "model", "gpt-5.6-luna",
                "usage", Map.of("input_tokens", 21, "output_tokens", 13),
                "output", List.of(Map.of(
                        "type", "message",
                        "content", List.of(Map.of("type", "output_text", "text", structured))))));

        server.expect(requestTo("http://openai.test/responses"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"model\":\"gpt-5.6-luna\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"store\":false")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"effort\":\"none\"")))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        AiModelResult<AiSearchProfile> result = client.analyze(
                List.of(new AiConversationTurn("user", "Tôi muốn học lập trình")),
                null,
                "vi",
                "safe-user");

        assertThat(result.model()).isEqualTo("gpt-5.6-luna");
        assertThat(result.inputTokens()).isEqualTo(21);
        assertThat(result.value().searchTerms()).containsExactly("programming");
        assertThat(result.value().maxPrice()).isEqualByComparingTo("300000");
        server.verify();
    }

    private OpenAiProperties properties(String key) {
        return new OpenAiProperties(
                key,
                "http://openai.test",
                "gpt-5.6-luna",
                Duration.ofSeconds(1),
                Duration.ofSeconds(3),
                30,
                10,
                30);
    }
}
