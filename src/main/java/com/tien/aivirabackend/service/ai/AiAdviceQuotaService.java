package com.tien.aivirabackend.service.ai;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tien.aivirabackend.config.properties.AiAdviceProperties;
import com.tien.aivirabackend.constant.AiAdviceUsageStatus;
import com.tien.aivirabackend.domain.dto.response.AiAdviceQuotaResponse;
import com.tien.aivirabackend.domain.entity.ai.AiAdviceMonthlyQuota;
import com.tien.aivirabackend.domain.entity.ai.AiAdviceUsage;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.AiAdviceErrorCode;
import com.tien.aivirabackend.exception.errorCode.UserErrorCode;
import com.tien.aivirabackend.repository.AiAdviceMonthlyQuotaRepository;
import com.tien.aivirabackend.repository.AiAdviceUsageRepository;
import com.tien.aivirabackend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiAdviceQuotaService {
    private static final ZoneId QUOTA_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final UserRepository userRepository;
    private final AiAdviceMonthlyQuotaRepository quotaRepository;
    private final AiAdviceUsageRepository usageRepository;
    private final AiAdviceProperties properties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long reserve(String userId, String clientMessageId) {
        String period = currentPeriod();
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND));
        AiAdviceUsage existing = usageRepository
                .findByUserIdAndPeriodKeyAndClientMessageId(userId, period, clientMessageId).orElse(null);
        if (existing != null && existing.getStatus() == AiAdviceUsageStatus.RESERVED) {
            throw new AppException(AiAdviceErrorCode.REQUEST_IN_PROGRESS);
        }
        if (existing != null && existing.getStatus() == AiAdviceUsageStatus.SUCCEEDED) {
            return existing.getId();
        }

        AiAdviceMonthlyQuota quota = quotaRepository.findForUpdate(userId, period)
                .orElseGet(() -> quotaRepository.saveAndFlush(AiAdviceMonthlyQuota.builder().user(user)
                        .periodKey(period).usedCount(0).reservedCount(0).build()));
        if (quota.getUsedCount() + quota.getReservedCount() >= properties.monthlyLimit()) {
            AiAdviceQuotaResponse details = toResponse(quota);
            throw new AppException(AiAdviceErrorCode.MONTHLY_LIMIT_REACHED, Map.of(), Map.of("limit", details.limit(),
                    "used", details.used(), "remaining", details.remaining(), "resetsAt", details.resetsAt()), null);
        }

        quota.setReservedCount(quota.getReservedCount() + 1);
        quotaRepository.save(quota);
        AiAdviceUsage usage = existing == null ? AiAdviceUsage.builder().user(user).periodKey(period)
                .clientMessageId(clientMessageId).status(AiAdviceUsageStatus.RESERVED).build() : existing;
        usage.setStatus(AiAdviceUsageStatus.RESERVED);
        return usageRepository.save(usage).getId();
    }

    @Transactional
    public void complete(Long usageId) {
        transition(usageId, AiAdviceUsageStatus.SUCCEEDED);
    }

    @Transactional
    public void fail(Long usageId) {
        transition(usageId, AiAdviceUsageStatus.FAILED);
    }

    @Transactional(readOnly = true)
    public AiAdviceQuotaResponse getQuota(String userId) {
        return quotaRepository.findByUserIdAndPeriodKey(userId, currentPeriod()).map(this::toResponse)
                .orElseGet(() -> new AiAdviceQuotaResponse(properties.monthlyLimit(), 0, properties.monthlyLimit(),
                        resetInstant()));
    }

    @Transactional
    public void releaseStaleReservations(Instant cutoff) {
        for (AiAdviceUsage usage : usageRepository.findByStatusAndUpdatedAtBefore(AiAdviceUsageStatus.RESERVED,
                cutoff)) {
            AiAdviceMonthlyQuota quota = quotaRepository.findForUpdate(usage.getUser().getId(), usage.getPeriodKey())
                    .orElse(null);
            if (quota != null) {
                quota.setReservedCount(Math.max(0, quota.getReservedCount() - 1));
            }
            usage.setStatus(AiAdviceUsageStatus.FAILED);
        }
    }

    private void transition(Long usageId, AiAdviceUsageStatus target) {
        AiAdviceUsage usage = usageRepository.findById(usageId).orElse(null);
        if (usage == null || usage.getStatus() != AiAdviceUsageStatus.RESERVED) {
            return;
        }
        AiAdviceMonthlyQuota quota = quotaRepository.findForUpdate(usage.getUser().getId(), usage.getPeriodKey())
                .orElseThrow(() -> new AppException(AiAdviceErrorCode.AI_ADVISOR_UNAVAILABLE));
        quota.setReservedCount(Math.max(0, quota.getReservedCount() - 1));
        if (target == AiAdviceUsageStatus.SUCCEEDED) {
            quota.setUsedCount(quota.getUsedCount() + 1);
        }
        usage.setStatus(target);
    }

    private AiAdviceQuotaResponse toResponse(AiAdviceMonthlyQuota quota) {
        int remaining = Math.max(0, properties.monthlyLimit() - quota.getUsedCount() - quota.getReservedCount());
        return new AiAdviceQuotaResponse(properties.monthlyLimit(), quota.getUsedCount(), remaining, resetInstant());
    }

    private String currentPeriod() {
        return YearMonth.now(QUOTA_ZONE).toString();
    }

    private Instant resetInstant() {
        ZonedDateTime now = ZonedDateTime.now(QUOTA_ZONE);
        return now.toLocalDate().withDayOfMonth(1).plusMonths(1).atStartOfDay(QUOTA_ZONE).toInstant();
    }
}
