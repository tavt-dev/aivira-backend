package com.tien.aivirabackend.service;

import com.tien.aivirabackend.constant.OtpType;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.entity.user.UserOtp;

public interface UserOtpService {

    UserOtp createOtp(User user, OtpType type, int expiryMinutes);

    void validateOtp(UserOtp userOtp, String providedOtp);

    void markOtpAsUsed(UserOtp userOtp);

    UserOtp findLatestOtp(User user, OtpType type);

    void checkOtpFrequency(User user, OtpType type);

    void deactivateOldOtps(String userId, OtpType type);
}
