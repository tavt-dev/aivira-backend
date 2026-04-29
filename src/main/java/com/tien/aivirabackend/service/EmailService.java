package com.tien.aivirabackend.service;

public interface EmailService {
    void sendRegistrationOtpByEmail(String to, String name, String otp);

    void sendForgotPasswordOtpByEmail(String to, String name, String otp);
}
