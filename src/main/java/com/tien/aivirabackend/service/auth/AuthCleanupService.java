package com.tien.aivirabackend.service.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tien.aivirabackend.repository.OAuthLoginStateRepository;
import com.tien.aivirabackend.repository.OAuthLoginTicketRepository;
import com.tien.aivirabackend.repository.RefreshTokenRepository;
import com.tien.aivirabackend.repository.UserOtpRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "AUTH-CLEANUP-SERVICE")
public class AuthCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserOtpRepository userOtpRepository;
    private final OAuthLoginStateRepository oAuthLoginStateRepository;
    private final OAuthLoginTicketRepository oAuthLoginTicketRepository;

    @Value("${auth.cleanup.otp-used-retention-hours:24}")
    private long otpUsedRetentionHours;

    @Value("${auth.cleanup.oauth-used-retention-hours:1}")
    private long oauthUsedRetentionHours;

    @Transactional
    @Scheduled(cron = "${auth.cleanup.cron:0 0 * * * *}")
    public void cleanupAuthData() {
        Instant now = Instant.now();
        Instant usedBefore = now.minus(otpUsedRetentionHours, ChronoUnit.HOURS);
        Instant oauthUsedBefore = now.minus(oauthUsedRetentionHours, ChronoUnit.HOURS);

        int expiredRefreshTokens = refreshTokenRepository.deleteExpiredTokens(now);
        int expiredOrUsedOtps = userOtpRepository.deleteExpiredOrUsedBefore(now, usedBefore);
        int expiredOrUsedOauthStates = oAuthLoginStateRepository.deleteExpiredOrConsumedBefore(now, oauthUsedBefore);
        int expiredOrUsedOauthTickets = oAuthLoginTicketRepository.deleteExpiredOrConsumedBefore(now, oauthUsedBefore);

        log.info(
                "auth_cleanup completed expiredRefreshTokens={} expiredOrUsedOtps={} expiredOrUsedOauthStates={} expiredOrUsedOauthTickets={} usedBefore={} oauthUsedBefore={}",
                expiredRefreshTokens,
                expiredOrUsedOtps,
                expiredOrUsedOauthStates,
                expiredOrUsedOauthTickets,
                usedBefore,
                oauthUsedBefore);
    }
}
