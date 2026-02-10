package com.tien.aivirabackend.service;

import com.nimbusds.jwt.SignedJWT;
import com.tien.aivirabackend.domain.dto.response.ActiveSessionResponse;
import com.tien.aivirabackend.domain.entity.user.User;

import java.util.List;

public interface JwtService {
    // Tạo Token
    String createAccessToken(User user);

    // Làm mới Token
    String createRefreshToken(User user, String deviceInfo, String ipAddress, String familyId);

    // Xác thực Token
    SignedJWT verifyAccessToken(String token);

    SignedJWT verifyRefreshToken(String refreshToken);

    void revokeRefreshToken(String refreshToken);

    void revokeAllTokensOfUser(String userId);

    String getTokenFamilyId(String refreshToken);

    void revokeSession(String userId, String sessionId);

    List<ActiveSessionResponse> getActiveSessions(String userId, String currentSessionJti);
}
