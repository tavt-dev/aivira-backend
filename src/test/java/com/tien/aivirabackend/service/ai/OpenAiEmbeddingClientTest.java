package com.tien.aivirabackend.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.tien.aivirabackend.config.properties.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class OpenAiEmbeddingClientTest {
    @Test
    void embedQuery_shouldUseConfiguredModelDimensionsAndNormalize() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://openai.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiProperties openAi = new OpenAiProperties("key", "http://openai.test", "chat-model",
                Duration.ofSeconds(1), Duration.ofSeconds(2));
        RagProperties rag = new RagProperties(true, null, null, null, 10, 0d, 10, 2, false, null, null,
                null, null, "text-embedding-3-small", 2);
        OpenAiEmbeddingClient client = new OpenAiEmbeddingClient(builder.build(), openAi, rag,
                new SimpleMeterRegistry());
        server.expect(requestTo("http://openai.test/embeddings"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"dimensions\":2")))
                .andRespond(withSuccess("{\"data\":[{\"embedding\":[3.0,4.0],\"index\":0}]}\n",
                        MediaType.APPLICATION_JSON));

        assertThat(client.embedQuery("semantic query")).containsExactly(0.6d, 0.8d);
        server.verify();
    }
}
