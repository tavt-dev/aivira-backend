package com.tien.aivirabackend.repository;

import com.tien.aivirabackend.constant.OtpType;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.entity.user.UserOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface UserOtpRepository extends JpaRepository<UserOtp, Long> {

    Optional<UserOtp> findTopByUserAndOtpTypeAndUsedFalseOrderByCreatedAtDesc(User user, OtpType type);

    @Modifying
    @Query("UPDATE UserOtp u SET u.used = true WHERE u.user.id = :userId AND u.otpType = :type AND u.used = false")
    void deactivateOldOtp(@Param("userId") String userId, @Param("type") OtpType type);

    @Modifying
    @Query("DELETE FROM UserOtp u WHERE u.expiresTime < :now OR (u.used = true AND u.usedAt < :usedBefore)")
    int deleteExpiredOrUsedBefore(@Param("now") Instant now, @Param("usedBefore") Instant usedBefore);
}
