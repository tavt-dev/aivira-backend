package com.tien.aivirabackend.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.EmailErrorCode;

class EmailServiceTest {
    BrevoEmailClient brevoEmailClient;
    EmailService emailService;

    @BeforeEach
    void setUp() {
        brevoEmailClient = mock(BrevoEmailClient.class);
        emailService = new EmailService(brevoEmailClient, new DefaultResourceLoader());
        ReflectionTestUtils.setField(emailService, "fromEmail", "sender@example.com");
        ReflectionTestUtils.setField(emailService, "fromName", "Aivira Store");
    }

    @Test
    void sendRegistrationOtpByEmail_shouldLoadTemplateAndSendViaBrevo() {
        emailService.sendRegistrationOtpByEmail("alice@example.com", "Alice", "123456");

        EmailMessage message = captureMessage();
        assertThat(message.toEmail()).isEqualTo("alice@example.com");
        assertThat(message.toName()).isEqualTo("Alice");
        assertThat(message.fromEmail()).isEqualTo("sender@example.com");
        assertThat(message.fromName()).isEqualTo("Aivira Store");
        assertThat(message.subject()).contains("Aivira");
        assertThat(message.htmlContent()).contains("Alice").contains("123456");
        assertThat(message.tag()).isEqualTo("registration-otp");
    }

    @Test
    void sendForgotPasswordOtpByEmail_shouldLoadTemplateAndSendViaBrevo() {
        emailService.sendForgotPasswordOtpByEmail("alice@example.com", "Alice", "654321");

        EmailMessage message = captureMessage();
        assertThat(message.toEmail()).isEqualTo("alice@example.com");
        assertThat(message.htmlContent()).contains("Alice").contains("654321");
        assertThat(message.tag()).isEqualTo("forgot-password-otp");
    }

    @Test
    void sendRegistrationOtpByEmail_whenTemplateCannotBeRead_shouldThrowAppException() throws IOException {
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        Resource resource = mock(Resource.class);
        when(resourceLoader.getResource(any())).thenReturn(resource);
        when(resource.getInputStream()).thenThrow(new IOException("template missing"));
        EmailService failingService = new EmailService(brevoEmailClient, resourceLoader);
        ReflectionTestUtils.setField(failingService, "fromEmail", "sender@example.com");
        ReflectionTestUtils.setField(failingService, "fromName", "Aivira Store");

        assertThatThrownBy(() -> failingService.sendRegistrationOtpByEmail("alice@example.com", "Alice", "123456"))
                .isInstanceOf(AppException.class).satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(EmailErrorCode.EMAIL_SEND_FAILED));
    }

    private EmailMessage captureMessage() {
        ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
        verify(brevoEmailClient).sendHtmlEmail(captor.capture());
        return captor.getValue();
    }
}
