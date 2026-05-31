package com.tien.aivirabackend.service.auth;

import java.text.ParseException;

import org.springframework.stereotype.Component;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.JwtErrorCode;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j(topic = "JWT-CLAIM-READER")
public class JwtClaimReader {
    public JWTClaimsSet parseClaims(String token, JwtErrorCode errorCode, String operation) {
        try {
            return SignedJWT.parse(token).getJWTClaimsSet();
        } catch (ParseException e) {
            log.error("Failed to parse JWT claims for {}", operation, e);
            throw new AppException(errorCode);
        }
    }

    public JWTClaimsSet getClaimsSet(SignedJWT signedJWT, JwtErrorCode errorCode, String operation) {
        try {
            return signedJWT.getJWTClaimsSet();
        } catch (ParseException e) {
            log.error("Failed to parse JWT claims for {}", operation, e);
            throw new AppException(errorCode);
        }
    }

    public String getStringClaim(JWTClaimsSet claimsSet, String claimName, String operation) {
        try {
            return claimsSet.getStringClaim(claimName);
        } catch (ParseException e) {
            log.error("Failed to read JWT claim '{}' for {}", claimName, operation, e);
            throw new AppException(JwtErrorCode.TOKEN_INVALID);
        }
    }
}
