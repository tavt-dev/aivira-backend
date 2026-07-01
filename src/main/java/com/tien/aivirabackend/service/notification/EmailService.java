package com.tien.aivirabackend.service.notification;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.EmailErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "EMAIL-SERVICE")
public class EmailService {

    private final BrevoEmailClient brevoEmailClient;
    private final ResourceLoader resourceLoader;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Async("emailTaskExecutor")
    public void sendRegistrationOtpByEmail(String to, String name, String otp) {
        String subject = "X\u00e1c th\u1ef1c t\u00e0i kho\u1ea3n Aivira - M\u00e3 OTP c\u1ee7a b\u1ea1n";
        String htmlContent =
                loadTemplate("registration-otp").replace("{{name}}", name).replace("{{otp}}", otp);

        sendHtmlEmail(to, name, subject, htmlContent, "registration-otp");
        log.info("Registration OTP email sent to: {}", to);
    }

    @Async("emailTaskExecutor")
    public void sendForgotPasswordOtpByEmail(String to, String name, String otp) {
        String subject = "\u0110\u1eb7t l\u1ea1i m\u1eadt kh\u1ea9u Aivira - M\u00e3 OTP c\u1ee7a b\u1ea1n";
        String htmlContent =
                loadTemplate("forgot-password-otp").replace("{{name}}", name).replace("{{otp}}", otp);

        sendHtmlEmail(to, name, subject, htmlContent, "forgot-password-otp");
        log.info("Forgot password OTP email sent to: {}", to);
    }

    private String loadTemplate(String templateName) {
        try {
            var resource = resourceLoader.getResource("classpath:templates/email/" + templateName + ".html");
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.error("Failed to load email template: {}", templateName, e);
            throw new AppException(EmailErrorCode.EMAIL_SEND_FAILED, e);
        }
    }

    private void sendHtmlEmail(String to, String name, String subject, String htmlContent, String tag) {
        EmailMessage message = new EmailMessage(to, name, fromEmail, fromName, subject, htmlContent, tag);
        brevoEmailClient.sendHtmlEmail(message);
        log.info("Email sent successfully to: {}", to);
    }
}
