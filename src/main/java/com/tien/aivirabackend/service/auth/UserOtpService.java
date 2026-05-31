package com.tien.aivirabackend.service.auth;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tien.aivirabackend.constant.OtpType;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.entity.user.UserOtp;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.OtpErrorCode;
import com.tien.aivirabackend.repository.UserOtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "USER_OTP-SERVICE")
public class UserOtpService {

    private final UserOtpRepository userOtpRepository;
    @Transactional
    public UserOtp createOtp(User user, OtpType type, int expiryMinutes) {
        String otpCode = generateSecureOtp();

        UserOtp userOtp = UserOtp.builder()
                .user(user)
                .otpCode(otpCode)
                .otpType(type)
                .expiresTime(Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES))
                .used(false)
                .build();

        UserOtp saved = userOtpRepository.save(userOtp);

        log.info("OTP created for user: {}, type: {}, expires: {}", user.getUsername(), type, saved.getExpiresTime());
        return saved;
    }
    @Transactional
    public void validateOtp(UserOtp userOtp, String providedOtp) {
        if (userOtp.getExpiresTime().isBefore(Instant.now())) {
            log.warn("OTP expired for user: {}", userOtp.getUser().getUsername());
            throw new AppException(OtpErrorCode.OTP_EXPIRED);
        }

        if (!userOtp.getOtpCode().equals(providedOtp)) {
            log.warn("Invalid OTP provided for user: {}", userOtp.getUser().getUsername());
            throw new AppException(OtpErrorCode.OTP_INVALID);
        }

        userOtp.setUsed(true);
        userOtp.setUsedAt(Instant.now());
        userOtpRepository.save(userOtp);

        log.info("OTP validated successfully for user: {}", userOtp.getUser().getUsername());
    }
    @Transactional
    public void markOtpAsUsed(UserOtp userOtp) {
        userOtp.setUsed(true);
        if (userOtp.getUsedAt() == null) {
            userOtp.setUsedAt(Instant.now());
        }
        userOtpRepository.save(userOtp);
    }
    public UserOtp findLatestOtp(User user, OtpType type) {
        return userOtpRepository
                .findTopByUserAndOtpTypeAndUsedFalseOrderByCreatedAtDesc(user, type)
                .orElseThrow(() -> new AppException(OtpErrorCode.OTP_NOT_FOUND));
    }
    public void checkOtpFrequency(User user, OtpType type) {
        Optional<UserOtp> lastOtp =
                userOtpRepository.findTopByUserAndOtpTypeAndUsedFalseOrderByCreatedAtDesc(user, type);
        if (lastOtp.isPresent()
                && lastOtp.get().getCreatedAt().isAfter(Instant.now().minus(1, ChronoUnit.MINUTES))) {
            log.warn("OTP request too frequent for user: {}, type: {}", user.getUsername(), type);
            throw new AppException(OtpErrorCode.OTP_REQUEST_TOO_FREQUENT);
        }
    }
    public void deactivateOldOtps(String userId, OtpType type) {
        userOtpRepository.deactivateOldOtp(userId, type);
    }

    private String generateSecureOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000); // [100000, 999999]
        return String.valueOf(otp);
    }
}
