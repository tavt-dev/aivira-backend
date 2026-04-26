package com.tien.aivirabackend.service;

import com.nimbusds.jwt.SignedJWT;
import com.tien.aivirabackend.constant.RevocationReason;
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

    void revokeRefreshToken(String refreshToken, RevocationReason reason, String replacedBy);

    void revokeAllTokensOfUser(String userId);

    void revokeAllTokensOfUser(String userId, RevocationReason reason);

    String getTokenFamilyId(String refreshToken);

    String getTokenJti(String token);

    void revokeSession(String userId, String sessionId);

    List<ActiveSessionResponse> getActiveSessions(String userId, String currentSessionJti);
}
