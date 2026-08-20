package com.tien.aivirabackend.service.notification;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.tien.aivirabackend.config.properties.BrevoProperties;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.EmailErrorCode;

class BrevoEmailClientTest {
    @Test
    void sendHtmlEmail_shouldPostExpectedPayloadToBrevo() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        BrevoEmailClient client = new BrevoEmailClient(properties(), builder.build());

        server.expect(once(), requestTo("https://api.brevo.com/v3/smtp/email"))
                .andExpect(header("api-key", "test-api-key"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("\"email\":\"sender@example.com\"")))
                .andExpect(content().string(containsString("\"name\":\"Aivira Store\"")))
                .andExpect(content().string(containsString("\"email\":\"alice@example.com\"")))
                .andExpect(content().string(containsString("\"name\":\"Alice\"")))
                .andExpect(content().string(containsString("\"subject\":\"Subject\"")))
                .andExpect(content().string(containsString("\"htmlContent\":\"<p>Hello</p>\"")))
                .andExpect(content().string(containsString("\"registration-otp\"")))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"messageId\":\"message-1\"}"));

        client.sendHtmlEmail(message());

        server.verify();
    }

    @Test
    void sendHtmlEmail_whenBrevoReturnsError_shouldThrowAppException() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        BrevoEmailClient client = new BrevoEmailClient(properties(), builder.build());

        server.expect(once(), requestTo("https://api.brevo.com/v3/smtp/email"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"invalid api key\"}"));

        assertThatThrownBy(() -> client.sendHtmlEmail(message())).isInstanceOf(AppException.class)
                .satisfies(ex -> org.assertj.core.api.Assertions.assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(EmailErrorCode.EMAIL_SEND_FAILED));

        server.verify();
    }

    @Test
    void sendHtmlEmail_whenApiKeyMissing_shouldThrowAppExceptionWithoutCallingBrevo() {
        BrevoProperties properties = properties();
        properties.setApiKey("");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        BrevoEmailClient client = new BrevoEmailClient(properties, builder.build());

        assertThatThrownBy(() -> client.sendHtmlEmail(message())).isInstanceOf(AppException.class)
                .satisfies(ex -> org.assertj.core.api.Assertions.assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(EmailErrorCode.EMAIL_SEND_FAILED));

        server.verify();
    }

    private BrevoProperties properties() {
        BrevoProperties properties = new BrevoProperties();
        properties.setApiKey("test-api-key");
        properties.setBaseUrl("https://api.brevo.com");
        properties.setSendEmailPath("/v3/smtp/email");
        return properties;
    }

    private EmailMessage message() {
        return new EmailMessage("alice@example.com", "Alice", "sender@example.com", "Aivira Store", "Subject",
                "<p>Hello</p>", "registration-otp");
    }
}
