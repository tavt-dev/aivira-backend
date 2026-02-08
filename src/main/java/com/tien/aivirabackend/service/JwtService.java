package com.tien.aivirabackend.service;

import com.nimbusds.jwt.SignedJWT;
import com.tien.aivirabackend.domain.entity.user.User;

public interface JwtService {
    // Tạo Token
    String createAccessToken(User user);

    // Làm mới Token
    String createRefreshToken(User user);

    // Xác thực Token
    SignedJWT verifyAccessToken(String token);

    SignedJWT verifyRefreshToken(String token);

    void revokeRefreshToken(String refreshToken);

    void revokeAllTokensOfUser(String userId);
}
