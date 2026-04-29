package com.tien.aivirabackend.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.EmailErrorCode;
import com.tien.aivirabackend.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "EMAIL-SERVICE")
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;
    private final ResourceLoader resourceLoader;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Override
    @Async("emailTaskExecutor")
    public void sendRegistrationOtpByEmail(String to, String name, String otp) {
        String subject = "Xác thực tài khoản Aivira - Mã OTP của bạn";
        String htmlContent =
                loadTemplate("registration-otp").replace("{{name}}", name).replace("{{otp}}", otp);

        sendHtmlEmail(to, subject, htmlContent);
        log.info("Registration OTP email sent to: {}", to);
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendForgotPasswordOtpByEmail(String to, String name, String otp) {
        String subject = "Đặt lại mật khẩu Aivira - Mã OTP của bạn";
        String htmlContent =
                loadTemplate("forgot-password-otp").replace("{{name}}", name).replace("{{otp}}", otp);

        sendHtmlEmail(to, subject, htmlContent);
        log.info("Forgot password OTP email sent to: {}", to);
    }

    // ===== Private helpers =====

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

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            javaMailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send email to: {}. Error: {}", to, e.getMessage(), e);
            throw new AppException(EmailErrorCode.EMAIL_SEND_FAILED, e);
        } catch (java.io.UnsupportedEncodingException e) {
            log.error("Unsupported encoding when sending email to: {}", to, e);
            throw new AppException(EmailErrorCode.EMAIL_SEND_FAILED, e);
        }
    }
}
