package com.tien.aivirabackend.service.notification;

public record EmailMessage(String toEmail, String toName, String fromEmail, String fromName, String subject,
        String htmlContent, String tag) {
}
