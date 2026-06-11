package com.tien.aivirabackend.service.auth;

public record GoogleUserInfo(
        String subject, String email, boolean emailVerified, String firstName, String lastName, String pictureUrl) {}
