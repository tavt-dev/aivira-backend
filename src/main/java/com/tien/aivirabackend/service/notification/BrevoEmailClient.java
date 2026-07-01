package com.tien.aivirabackend.service.notification;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.tien.aivirabackend.config.properties.BrevoProperties;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.EmailErrorCode;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j(topic = "BREVO-EMAIL-CLIENT")
public class BrevoEmailClient {
    BrevoProperties brevoProperties;
    RestClient brevoRestClient;

    public void sendHtmlEmail(EmailMessage message) {
        validateConfiguration();

        Map<String, Object> sender = new LinkedHashMap<>();
        sender.put("email", message.fromEmail());
        sender.put("name", message.fromName());

        Map<String, Object> recipient = new LinkedHashMap<>();
        recipient.put("email", message.toEmail());
        if (StringUtils.hasText(message.toName())) {
            recipient.put("name", message.toName());
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sender", sender);
        payload.put("to", List.of(recipient));
        payload.put("subject", message.subject());
        payload.put("htmlContent", message.htmlContent());
        payload.put("tags", List.of("aivira", message.tag()));

        try {
            Map<String, Object> response = brevoRestClient
                    .post()
                    .uri(brevoProperties.sendEmailUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("api-key", brevoProperties.getApiKey())
                    .body(payload)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            Object messageId = response == null ? null : response.get("messageId");
            log.info(
                    "brevo_email_sent recipient={} tag={} messageId={}",
                    message.toEmail(),
                    message.tag(),
                    messageId == null ? "" : messageId);
        } catch (RestClientResponseException ex) {
            log.warn(
                    "brevo_email_failed recipient={} tag={} status={}",
                    message.toEmail(),
                    message.tag(),
                    ex.getStatusCode().value());
            throw new AppException(EmailErrorCode.EMAIL_SEND_FAILED, ex);
        } catch (RestClientException ex) {
            log.warn("brevo_email_failed recipient={} tag={} status=network_error", message.toEmail(), message.tag());
            throw new AppException(EmailErrorCode.EMAIL_SEND_FAILED, ex);
        }
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(brevoProperties.getApiKey())) {
            throw new AppException(EmailErrorCode.EMAIL_SEND_FAILED);
        }
    }
}
