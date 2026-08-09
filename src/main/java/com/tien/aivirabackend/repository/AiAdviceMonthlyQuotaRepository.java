package com.tien.aivirabackend.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tien.aivirabackend.domain.entity.ai.AiAdviceMonthlyQuota;

public interface AiAdviceMonthlyQuotaRepository extends JpaRepository<AiAdviceMonthlyQuota, Long> {
    Optional<AiAdviceMonthlyQuota> findByUserIdAndPeriodKey(String userId, String periodKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from AiAdviceMonthlyQuota q where q.user.id = :userId and q.periodKey = :periodKey")
    Optional<AiAdviceMonthlyQuota> findForUpdate(
            @Param("userId") String userId, @Param("periodKey") String periodKey);
}
