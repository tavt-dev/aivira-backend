package com.tien.aivirabackend.service.auth;

public interface GoogleIdTokenVerifier {
    GoogleUserInfo verify(String idToken, String expectedAudience);
}
