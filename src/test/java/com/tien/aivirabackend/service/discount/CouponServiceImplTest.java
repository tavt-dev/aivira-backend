package com.tien.aivirabackend.service.discount;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tien.aivirabackend.constant.CouponType;
import com.tien.aivirabackend.domain.dto.request.CouponCreateRequest;
import com.tien.aivirabackend.domain.dto.response.CouponResponse;
import com.tien.aivirabackend.domain.entity.discount.Coupon;
import com.tien.aivirabackend.domain.mapper.DiscountMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.CouponErrorCode;
import com.tien.aivirabackend.exception.errorCode.PromotionErrorCode;
import com.tien.aivirabackend.repository.CouponRepository;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {
    @Mock
    CouponRepository couponRepository;

    CouponServiceImpl couponService;

    @BeforeEach
    void setUp() {
        couponService = new CouponServiceImpl(couponRepository, new DiscountMapper());
    }

    @Test
    void createCoupon_shouldTrimUppercaseCodeAndDefaultActive() {
        when(couponRepository.existsByCode("SAVE10")).thenReturn(false);
        when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CouponResponse response = couponService.createCoupon(createRequest(" save10 "));

        assertThat(response.getCode()).isEqualTo("SAVE10");
        assertThat(response.getUsedCount()).isZero();
        assertThat(response.getActive()).isTrue();
        verify(couponRepository).save(argThat(coupon -> coupon.getCode().equals("SAVE10")));
    }

    @Test
    void createCoupon_whenDuplicateCode_shouldThrow() {
        when(couponRepository.existsByCode("SAVE10")).thenReturn(true);

        assertThatThrownBy(() -> couponService.createCoupon(createRequest("save10")))
                .isInstanceOfSatisfying(AppException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(CouponErrorCode.COUPON_CODE_ALREADY_EXISTS));

        verify(couponRepository, never()).save(any());
    }

    @Test
    void createCoupon_whenInvalidPercent_shouldThrow() {
        CouponCreateRequest request = createRequest("SAVE10");
        request.setValue(BigDecimal.valueOf(101));

        assertThatThrownBy(() -> couponService.createCoupon(request)).isInstanceOfSatisfying(AppException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(PromotionErrorCode.DISCOUNT_INVALID_VALUE));
    }

    @Test
    void deleteCoupon_shouldDeactivateInsteadOfDeleting() {
        Coupon coupon = Coupon.builder().code("SAVE10").active(true).build();
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

        couponService.deleteCoupon(1L);

        assertThat(coupon.getActive()).isFalse();
        verify(couponRepository).save(coupon);
        verify(couponRepository, never()).delete(any());
    }

    private CouponCreateRequest createRequest(String code) {
        return CouponCreateRequest.builder().code(code).type(CouponType.PERCENT).value(BigDecimal.TEN)
                .maxDiscountAmount(BigDecimal.valueOf(50)).minOrderAmount(BigDecimal.ZERO).usageLimit(100)
                .usageLimitPerUser(1).startAt(LocalDateTime.now().minusDays(1)).endAt(LocalDateTime.now().plusDays(1))
                .build();
    }
}
