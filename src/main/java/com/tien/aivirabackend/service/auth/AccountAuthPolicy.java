package com.tien.aivirabackend.service.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.AccountErrorCode;
import com.tien.aivirabackend.exception.errorCode.UserErrorCode;
import com.tien.aivirabackend.repository.UserRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j(topic = "ACCOUNT-AUTH-POLICY")
public class AccountAuthPolicy {
    UserRepository userRepository;

    @NonFinal
    @Value("${auth.brute-force.max-attempts:5}")
    int maxFailedLoginAttempts;

    @NonFinal
    @Value("${auth.brute-force.window-minutes:15}")
    int failedLoginWindowMinutes;

    @NonFinal
    @Value("${auth.brute-force.lock-minutes:15}")
    int lockMinutes;

    public void validateAccountForAuth(User user) {
        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new AppException(AccountErrorCode.ACCOUNT_DELETED);
        }
        if (Boolean.TRUE.equals(user.getIsLocked())) {
            throw new AppException(AccountErrorCode.ACCOUNT_LOCKED);
        }

        Instant now = Instant.now();
        if (user.getLockoutUntil() != null && user.getLockoutUntil().isAfter(now)) {
            throw new AppException(AccountErrorCode.ACCOUNT_LOCKED);
        }
        if (user.getLockoutUntil() != null && !user.getLockoutUntil().isAfter(now)) {
            user.setLockoutUntil(null);
            user.setFailedLoginAttempts(0);
            user.setFirstFailedLoginAt(null);
            userRepository.save(user);
        }
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new AppException(AccountErrorCode.ACCOUNT_NOT_VERIFIED);
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AppException(UserErrorCode.USER_ACCOUNT_INACTIVE);
        }
    }

    public void registerFailedLoginAttempt(User user, String ipAddress, String deviceInfo) {
        Instant now = Instant.now();
        Instant boundary = now.minus(failedLoginWindowMinutes, ChronoUnit.MINUTES);

        Integer currentAttempts = Objects.requireNonNullElse(user.getFailedLoginAttempts(), 0);
        Instant firstFailedAt = user.getFirstFailedLoginAt();

        if (firstFailedAt == null || firstFailedAt.isBefore(boundary)) {
            firstFailedAt = now;
            currentAttempts = 0;
        }

        int newAttempts = currentAttempts + 1;
        user.setFirstFailedLoginAt(firstFailedAt);
        user.setFailedLoginAttempts(newAttempts);

        if (newAttempts >= maxFailedLoginAttempts) {
            user.setLockoutUntil(now.plus(lockMinutes, ChronoUnit.MINUTES));
            user.setFailedLoginAttempts(0);
            user.setFirstFailedLoginAt(null);
            log.warn(
                    "auth_login_lockout userId={} username={} ip={} device={} lockUntil={}",
                    user.getId(),
                    user.getUsername(),
                    ipAddress,
                    deviceInfo,
                    user.getLockoutUntil());
        }

        userRepository.save(user);
    }

    public void clearFailedLoginState(User user) {
        boolean changed = false;
        if (!Objects.equals(user.getFailedLoginAttempts(), 0)) {
            user.setFailedLoginAttempts(0);
            changed = true;
        }
        if (user.getFirstFailedLoginAt() != null) {
            user.setFirstFailedLoginAt(null);
            changed = true;
        }
        if (user.getLockoutUntil() != null) {
            user.setLockoutUntil(null);
            changed = true;
        }
        if (changed) {
            userRepository.save(user);
        }
    }
}
