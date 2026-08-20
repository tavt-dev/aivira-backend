package com.tien.aivirabackend.service.discount;

import java.math.BigDecimal;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.tien.aivirabackend.constant.CouponType;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.CouponCreateRequest;
import com.tien.aivirabackend.domain.dto.request.CouponUpdateRequest;
import com.tien.aivirabackend.domain.dto.response.CouponResponse;
import com.tien.aivirabackend.domain.entity.discount.Coupon;
import com.tien.aivirabackend.domain.mapper.DiscountMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.CouponErrorCode;
import com.tien.aivirabackend.exception.errorCode.PromotionErrorCode;
import com.tien.aivirabackend.repository.CouponRepository;
import com.tien.aivirabackend.util.PageRequestUtils;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CouponServiceImpl implements CouponService {
    CouponRepository couponRepository;
    DiscountMapper discountMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CouponResponse> getCoupons(int page, int size) {
        return PageResponse.from(couponRepository.findAll(PageRequestUtils.newestFirst(page, size))
                .map(discountMapper::toCouponResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public CouponResponse getCoupon(Long couponId) {
        return discountMapper.toCouponResponse(findCoupon(couponId));
    }

    @Override
    @Transactional
    public CouponResponse createCoupon(CouponCreateRequest request) {
        String code = normalizeCode(request.getCode());
        if (couponRepository.existsByCode(code)) {
            throw new AppException(CouponErrorCode.COUPON_CODE_ALREADY_EXISTS);
        }
        validateCoupon(request.getType(), request.getValue(), request.getMaxDiscountAmount(),
                request.getMinOrderAmount(), request.getUsageLimit(), request.getUsageLimitPerUser(),
                request.getStartAt(), request.getEndAt());
        Coupon coupon = Coupon.builder().code(code).type(request.getType()).value(request.getValue())
                .maxDiscountAmount(request.getMaxDiscountAmount()).minOrderAmount(request.getMinOrderAmount())
                .usageLimit(request.getUsageLimit()).usageLimitPerUser(request.getUsageLimitPerUser()).usedCount(0)
                .startAt(request.getStartAt()).endAt(request.getEndAt())
                .active(request.getActive() == null || Boolean.TRUE.equals(request.getActive())).build();
        return discountMapper.toCouponResponse(couponRepository.save(coupon));
    }

    @Override
    @Transactional
    public CouponResponse updateCoupon(Long couponId, CouponUpdateRequest request) {
        Coupon coupon = findCoupon(couponId);
        if (request.getCode() != null) {
            String code = normalizeCode(request.getCode());
            if (!code.equals(coupon.getCode()) && couponRepository.existsByCodeAndIdNot(code, couponId)) {
                throw new AppException(CouponErrorCode.COUPON_CODE_ALREADY_EXISTS);
            }
            coupon.setCode(code);
        }
        if (request.getType() != null) {
            coupon.setType(request.getType());
        }
        if (request.getValue() != null) {
            coupon.setValue(request.getValue());
        }
        if (request.getMaxDiscountAmount() != null) {
            coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
        }
        if (request.getMinOrderAmount() != null) {
            coupon.setMinOrderAmount(request.getMinOrderAmount());
        }
        if (request.getUsageLimit() != null) {
            coupon.setUsageLimit(request.getUsageLimit());
        }
        if (request.getUsageLimitPerUser() != null) {
            coupon.setUsageLimitPerUser(request.getUsageLimitPerUser());
        }
        if (request.getStartAt() != null) {
            coupon.setStartAt(request.getStartAt());
        }
        if (request.getEndAt() != null) {
            coupon.setEndAt(request.getEndAt());
        }
        if (request.getActive() != null) {
            coupon.setActive(request.getActive());
        }
        validateCoupon(coupon.getType(), coupon.getValue(), coupon.getMaxDiscountAmount(), coupon.getMinOrderAmount(),
                coupon.getUsageLimit(), coupon.getUsageLimitPerUser(), coupon.getStartAt(), coupon.getEndAt());
        return discountMapper.toCouponResponse(couponRepository.save(coupon));
    }

    @Override
    @Transactional
    public void deleteCoupon(Long couponId) {
        Coupon coupon = findCoupon(couponId);
        coupon.setActive(false);
        couponRepository.save(coupon);
    }

    private Coupon findCoupon(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new AppException(CouponErrorCode.COUPON_NOT_FOUND));
    }

    private void validateCoupon(CouponType type, BigDecimal value, BigDecimal maxDiscountAmount,
            BigDecimal minOrderAmount, Integer usageLimit, Integer usageLimitPerUser, java.time.LocalDateTime startAt,
            java.time.LocalDateTime endAt) {
        if (type == null || value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(PromotionErrorCode.DISCOUNT_INVALID_VALUE);
        }
        if (type == CouponType.PERCENT && value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new AppException(PromotionErrorCode.DISCOUNT_INVALID_VALUE);
        }
        if (maxDiscountAmount != null && maxDiscountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(PromotionErrorCode.DISCOUNT_INVALID_VALUE);
        }
        if (minOrderAmount != null && minOrderAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(PromotionErrorCode.DISCOUNT_INVALID_VALUE);
        }
        if (usageLimit != null && usageLimit <= 0) {
            throw new AppException(PromotionErrorCode.DISCOUNT_INVALID_VALUE);
        }
        if (usageLimitPerUser != null && usageLimitPerUser <= 0) {
            throw new AppException(PromotionErrorCode.DISCOUNT_INVALID_VALUE);
        }
        if (startAt == null || endAt == null || !startAt.isBefore(endAt)) {
            throw new AppException(CouponErrorCode.COUPON_INVALID);
        }
    }

    private String normalizeCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new AppException(CouponErrorCode.COUPON_INVALID);
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
