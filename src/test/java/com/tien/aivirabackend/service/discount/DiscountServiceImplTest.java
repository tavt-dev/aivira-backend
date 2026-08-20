package com.tien.aivirabackend.service.discount;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tien.aivirabackend.constant.CouponType;
import com.tien.aivirabackend.constant.CouponUsageStatus;
import com.tien.aivirabackend.constant.PromotionScope;
import com.tien.aivirabackend.constant.PromotionType;
import com.tien.aivirabackend.domain.entity.catalog.Category;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.entity.catalog.ProductVariation;
import com.tien.aivirabackend.domain.entity.discount.Coupon;
import com.tien.aivirabackend.domain.entity.discount.CouponUsage;
import com.tien.aivirabackend.domain.entity.discount.Promotion;
import com.tien.aivirabackend.domain.entity.transaction.CartItem;
import com.tien.aivirabackend.domain.entity.transaction.Order;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.mapper.DiscountMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.CouponErrorCode;
import com.tien.aivirabackend.repository.CouponRepository;
import com.tien.aivirabackend.repository.CouponUsageRepository;
import com.tien.aivirabackend.repository.PromotionRepository;

@ExtendWith(MockitoExtension.class)
class DiscountServiceImplTest {
    @Mock
    PromotionRepository promotionRepository;

    @Mock
    CouponRepository couponRepository;

    @Mock
    CouponUsageRepository couponUsageRepository;

    DiscountServiceImpl discountService;

    @BeforeEach
    void setUp() {
        discountService = new DiscountServiceImpl(promotionRepository, couponRepository, couponUsageRepository,
                new DiscountMapper());
    }

    @Test
    void calculate_shouldApplyBestPromotionThenCoupon() {
        User user = user();
        ProductVariation variation = variation();
        CartItem cartItem = cartItem(variation, 2);
        Promotion productPromotion = promotion(1L, "Product 10%", PromotionScope.PRODUCT, 20L, PromotionType.PERCENT,
                "10");
        Promotion categoryPromotion = promotion(2L, "Category 15K", PromotionScope.CATEGORY, 10L, PromotionType.FIXED,
                "15");
        Coupon coupon = coupon("SAVE10", CouponType.PERCENT, "10", "25");

        when(promotionRepository.findActiveAt(any())).thenReturn(List.of(productPromotion, categoryPromotion));
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));

        DiscountCalculation calculation = discountService.calculate(user, List.of(cartItem), Map.of(30L, variation),
                " save10 ");

        assertThat(calculation.subtotal()).isEqualByComparingTo("200.00");
        assertThat(calculation.promotionDiscountAmount()).isEqualByComparingTo("30.00");
        assertThat(calculation.couponDiscountAmount()).isEqualByComparingTo("17.00");
        assertThat(calculation.discountAmount()).isEqualByComparingTo("47.00");
        assertThat(calculation.totalAmount()).isEqualByComparingTo("153.00");
        assertThat(calculation.items().getFirst().promotionName()).isEqualTo("Category 15K");
        assertThat(discountService.toPreviewResponse(calculation).getAppliedPromotions().getFirst().getDiscountAmount())
                .isEqualByComparingTo("30.00");
    }

    @Test
    void calculate_whenUsageLimitConsumedByReservedOrFinalized_shouldThrow() {
        User user = user();
        ProductVariation variation = variation();
        CartItem cartItem = cartItem(variation, 1);
        Coupon coupon = coupon("SAVE10", CouponType.PERCENT, "10", null);
        coupon.setUsageLimit(1);
        when(promotionRepository.findActiveAt(any())).thenReturn(List.of());
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));
        when(couponUsageRepository.countByCoupon_IdAndStatusIn(eq(1L), anyCollection())).thenReturn(1L);

        assertThatThrownBy(() -> discountService.calculate(user, List.of(cartItem), Map.of(30L, variation), "SAVE10"))
                .isInstanceOfSatisfying(AppException.class, exception -> assertThat(exception.getErrorCode())
                        .isEqualTo(CouponErrorCode.COUPON_USAGE_LIMIT_EXCEEDED));
    }

    @Test
    void reserveOrFinalizeCoupon_whenOnlineCheckout_shouldCreateReservedUsageOnly() {
        User user = user();
        Order order = order(99L);
        Coupon coupon = coupon("SAVE10", CouponType.FIXED, "20", null);
        DiscountCalculation calculation = calculation(coupon, "20.00");
        when(couponRepository.findByCodeForUpdate("SAVE10")).thenReturn(Optional.of(coupon));

        discountService.reserveOrFinalizeCoupon(user, order, calculation, false);

        ArgumentCaptor<CouponUsage> usageCaptor = ArgumentCaptor.forClass(CouponUsage.class);
        verify(couponUsageRepository).save(usageCaptor.capture());
        CouponUsage usage = usageCaptor.getValue();
        assertThat(usage.getStatus()).isEqualTo(CouponUsageStatus.RESERVED);
        assertThat(usage.getUsedAt()).isNull();
        assertThat(coupon.getUsedCount()).isZero();
        verify(couponRepository, never()).save(any());
    }

    @Test
    void reserveOrFinalizeCoupon_whenCodCheckout_shouldFinalizeAndIncrementUsedCount() {
        User user = user();
        Order order = order(99L);
        Coupon coupon = coupon("SAVE10", CouponType.FIXED, "20", null);
        DiscountCalculation calculation = calculation(coupon, "20.00");
        when(couponRepository.findByCodeForUpdate("SAVE10")).thenReturn(Optional.of(coupon));

        discountService.reserveOrFinalizeCoupon(user, order, calculation, true);

        ArgumentCaptor<CouponUsage> usageCaptor = ArgumentCaptor.forClass(CouponUsage.class);
        verify(couponUsageRepository).save(usageCaptor.capture());
        assertThat(usageCaptor.getValue().getStatus()).isEqualTo(CouponUsageStatus.FINALIZED);
        assertThat(usageCaptor.getValue().getUsedAt()).isNotNull();
        assertThat(coupon.getUsedCount()).isEqualTo(1);
        verify(couponRepository).save(coupon);
    }

    @Test
    void finalizeReservedCouponUsagesForOrders_shouldFinalizeExactlyOnce() {
        Coupon coupon = coupon("SAVE10", CouponType.FIXED, "20", null);
        CouponUsage usage = CouponUsage.builder().coupon(coupon).order(order(99L)).status(CouponUsageStatus.RESERVED)
                .discountAmount(BigDecimal.valueOf(20)).build();
        when(couponUsageRepository.findByOrder_IdInAndStatus(List.of(99L), CouponUsageStatus.RESERVED))
                .thenReturn(List.of(usage));
        when(couponRepository.findByCodeForUpdate("SAVE10")).thenReturn(Optional.of(coupon));

        discountService.finalizeReservedCouponUsagesForOrders(List.of(order(99L)));

        assertThat(usage.getStatus()).isEqualTo(CouponUsageStatus.FINALIZED);
        assertThat(usage.getUsedAt()).isNotNull();
        assertThat(coupon.getUsedCount()).isEqualTo(1);
    }

    @Test
    void releaseReservedCouponUsagesForOrders_shouldReleaseWithoutIncrementingUsedCount() {
        Coupon coupon = coupon("SAVE10", CouponType.FIXED, "20", null);
        CouponUsage usage = CouponUsage.builder().coupon(coupon).order(order(99L)).status(CouponUsageStatus.RESERVED)
                .discountAmount(BigDecimal.valueOf(20)).build();
        when(couponUsageRepository.findByOrder_IdInAndStatus(List.of(99L), CouponUsageStatus.RESERVED))
                .thenReturn(List.of(usage));

        discountService.releaseReservedCouponUsagesForOrders(List.of(order(99L)));

        assertThat(usage.getStatus()).isEqualTo(CouponUsageStatus.RELEASED);
        assertThat(usage.getReleasedAt()).isNotNull();
        assertThat(coupon.getUsedCount()).isZero();
    }

    private User user() {
        return User.builder().id("user-1").build();
    }

    private ProductVariation variation() {
        Category category = Category.builder().id(10L).categoryName("Fiction").build();
        Product product = Product.builder().id(20L).productName("Clean Architecture").price(BigDecimal.valueOf(100))
                .category(category).build();
        return ProductVariation.builder().id(30L).sku("BOOK-1-PB").additionalPrice(BigDecimal.ZERO).product(product)
                .build();
    }

    private CartItem cartItem(ProductVariation variation, int quantity) {
        return CartItem.builder().id(40L).productVariation(variation).quantity(quantity).build();
    }

    private Promotion promotion(Long id, String name, PromotionScope scope, Long targetId, PromotionType type,
            String value) {
        LocalDateTime now = LocalDateTime.now();
        return Promotion.builder().id(id).promotionName(name).promotionScope(scope).targetId(targetId)
                .promotionType(type).value(new BigDecimal(value)).startAt(now.minusDays(1)).endAt(now.plusDays(1))
                .active(true).build();
    }

    private Coupon coupon(String code, CouponType type, String value, String maxDiscountAmount) {
        LocalDateTime now = LocalDateTime.now();
        return Coupon.builder().id(1L).code(code).type(type).value(new BigDecimal(value))
                .maxDiscountAmount(maxDiscountAmount == null ? null : new BigDecimal(maxDiscountAmount)).usedCount(0)
                .startAt(now.minusDays(1)).endAt(now.plusDays(1)).active(true).build();
    }

    private DiscountCalculation calculation(Coupon coupon, String discountAmount) {
        return new DiscountCalculation(BigDecimal.valueOf(100), BigDecimal.ZERO, new BigDecimal(discountAmount),
                new BigDecimal(discountAmount), BigDecimal.ZERO, BigDecimal.valueOf(80), coupon.getCode(), coupon,
                List.of());
    }

    private Order order(Long id) {
        return Order.builder().id(id).subtotal(BigDecimal.valueOf(100)).discountAmount(BigDecimal.valueOf(20)).build();
    }
}
