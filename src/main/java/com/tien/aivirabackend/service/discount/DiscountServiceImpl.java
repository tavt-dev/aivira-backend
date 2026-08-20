package com.tien.aivirabackend.service.discount;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.tien.aivirabackend.constant.CouponType;
import com.tien.aivirabackend.constant.CouponUsageStatus;
import com.tien.aivirabackend.constant.PromotionScope;
import com.tien.aivirabackend.constant.PromotionType;
import com.tien.aivirabackend.domain.dto.response.AppliedPromotionResponse;
import com.tien.aivirabackend.domain.dto.response.CheckoutPreviewItemResponse;
import com.tien.aivirabackend.domain.dto.response.CheckoutPreviewResponse;
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

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DiscountServiceImpl implements DiscountService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final Collection<CouponUsageStatus> CONSUMING_STATUSES = List.of(CouponUsageStatus.RESERVED,
            CouponUsageStatus.FINALIZED);

    PromotionRepository promotionRepository;
    CouponRepository couponRepository;
    CouponUsageRepository couponUsageRepository;
    DiscountMapper discountMapper;

    @Override
    @Transactional(readOnly = true)
    public DiscountCalculation calculate(User user, List<CartItem> cartItems, Map<Long, ProductVariation> variations,
            String couponCode) {
        LocalDateTime now = LocalDateTime.now();
        List<Promotion> promotions = promotionRepository.findActiveAt(now);
        List<DiscountItem> items = new ArrayList<>();
        BigDecimal subtotal = ZERO;
        BigDecimal promotionDiscount = ZERO;

        for (CartItem cartItem : cartItems) {
            ProductVariation variation = variations.get(cartItem.getProductVariation().getId());
            Product product = variation.getProduct();
            BigDecimal unitPrice = money(product.getPrice().add(nullToZero(variation.getAdditionalPrice())));
            BigDecimal lineSubtotal = money(unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            Promotion bestPromotion = null;
            BigDecimal bestDiscount = ZERO;
            for (Promotion promotion : promotions) {
                if (matches(promotion, product)) {
                    BigDecimal discount = promotionDiscount(promotion, lineSubtotal, cartItem.getQuantity());
                    if (discount.compareTo(bestDiscount) > 0) {
                        bestDiscount = discount;
                        bestPromotion = promotion;
                    }
                }
            }
            BigDecimal finalLineAmount = money(lineSubtotal.subtract(bestDiscount).max(BigDecimal.ZERO));
            subtotal = subtotal.add(lineSubtotal);
            promotionDiscount = promotionDiscount.add(bestDiscount);
            items.add(new DiscountItem(cartItem, variation, unitPrice, lineSubtotal, bestDiscount,
                    bestPromotion == null ? null : bestPromotion.getId(),
                    bestPromotion == null ? null : bestPromotion.getPromotionName(), finalLineAmount));
        }

        subtotal = money(subtotal);
        promotionDiscount = money(promotionDiscount);
        BigDecimal afterPromotion = money(subtotal.subtract(promotionDiscount).max(BigDecimal.ZERO));
        Coupon coupon = resolveCoupon(user, couponCode, afterPromotion, now);
        BigDecimal couponDiscount = coupon == null ? ZERO : couponDiscount(coupon, afterPromotion);
        BigDecimal discountAmount = money(promotionDiscount.add(couponDiscount));
        BigDecimal totalAmount = money(subtotal.subtract(discountAmount).max(BigDecimal.ZERO));

        return new DiscountCalculation(subtotal, promotionDiscount, couponDiscount, discountAmount, ZERO, totalAmount,
                coupon == null ? null : coupon.getCode(), coupon, List.copyOf(items));
    }

    @Override
    public CheckoutPreviewResponse toPreviewResponse(DiscountCalculation calculation) {
        Map<Long, AppliedPromotionResponse> promotions = new LinkedHashMap<>();
        for (DiscountItem item : calculation.items()) {
            if (item.promotionId() != null && item.promotionDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                AppliedPromotionResponse existing = promotions.get(item.promotionId());
                BigDecimal amount = existing == null ? item.promotionDiscountAmount()
                        : existing.getDiscountAmount().add(item.promotionDiscountAmount());
                promotions.put(item.promotionId(), AppliedPromotionResponse.builder().promotionId(item.promotionId())
                        .promotionName(item.promotionName()).discountAmount(money(amount)).build());
            }
        }

        return CheckoutPreviewResponse.builder().subtotal(calculation.subtotal())
                .promotionDiscountAmount(calculation.promotionDiscountAmount())
                .couponDiscountAmount(calculation.couponDiscountAmount()).discountAmount(calculation.discountAmount())
                .shippingFee(calculation.shippingFee()).totalAmount(calculation.totalAmount())
                .couponCode(calculation.couponCode()).coupon(discountMapper.toCouponResponse(calculation.coupon()))
                .appliedPromotions(new ArrayList<>(promotions.values()))
                .items(calculation.items().stream().map(this::toPreviewItem).toList()).build();
    }

    @Override
    @Transactional
    public void reserveOrFinalizeCoupon(User user, Order order, DiscountCalculation calculation,
            boolean finalizeImmediately) {
        if (calculation.coupon() == null || calculation.couponDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Coupon coupon = couponRepository.findByCodeForUpdate(calculation.couponCode())
                .orElseThrow(() -> new AppException(CouponErrorCode.COUPON_NOT_FOUND));
        validateCouponAvailability(user, coupon, calculation.subtotal().subtract(calculation.promotionDiscountAmount()),
                LocalDateTime.now());
        LocalDateTime now = LocalDateTime.now();
        CouponUsage usage = CouponUsage.builder().coupon(coupon).user(user).order(order)
                .discountAmount(calculation.couponDiscountAmount())
                .status(finalizeImmediately ? CouponUsageStatus.FINALIZED : CouponUsageStatus.RESERVED).reservedAt(now)
                .finalizedAt(finalizeImmediately ? now : null).usedAt(finalizeImmediately ? now : null).build();
        if (finalizeImmediately) {
            coupon.setUsedCount(nullToZero(coupon.getUsedCount()) + 1);
            couponRepository.save(coupon);
        }
        couponUsageRepository.save(usage);
    }

    @Override
    @Transactional
    public void finalizeReservedCouponUsagesForOrders(List<Order> orders) {
        LocalDateTime now = LocalDateTime.now();
        for (CouponUsage usage : reservedUsages(orders)) {
            Coupon coupon = couponRepository.findByCodeForUpdate(usage.getCoupon().getCode())
                    .orElseThrow(() -> new AppException(CouponErrorCode.COUPON_NOT_FOUND));
            if (usage.getStatus() == CouponUsageStatus.RESERVED) {
                usage.setStatus(CouponUsageStatus.FINALIZED);
                usage.setFinalizedAt(now);
                usage.setUsedAt(now);
                coupon.setUsedCount(nullToZero(coupon.getUsedCount()) + 1);
                couponRepository.save(coupon);
                couponUsageRepository.save(usage);
            }
        }
    }

    @Override
    @Transactional
    public void releaseReservedCouponUsagesForOrders(List<Order> orders) {
        LocalDateTime now = LocalDateTime.now();
        for (CouponUsage usage : reservedUsages(orders)) {
            usage.setStatus(CouponUsageStatus.RELEASED);
            usage.setReleasedAt(now);
            couponUsageRepository.save(usage);
        }
    }

    @Override
    @Transactional
    public void reserveReleasedCouponUsagesForRetry(List<Order> orders) {
        LocalDateTime now = LocalDateTime.now();
        List<Long> orderIds = orderIds(orders);
        if (orderIds.isEmpty()) {
            return;
        }
        List<CouponUsage> releasedUsages = couponUsageRepository.findByOrder_IdInAndStatus(orderIds,
                CouponUsageStatus.RELEASED);
        for (CouponUsage usage : releasedUsages) {
            Coupon coupon = couponRepository.findByCodeForUpdate(usage.getCoupon().getCode())
                    .orElseThrow(() -> new AppException(CouponErrorCode.COUPON_NOT_FOUND));
            BigDecimal eligibleAmount = usage.getOrder().getSubtotal().subtract(usage.getOrder().getDiscountAmount())
                    .add(usage.getDiscountAmount());
            validateCouponAvailability(usage.getUser(), coupon, eligibleAmount, now);
            usage.setStatus(CouponUsageStatus.RESERVED);
            usage.setReservedAt(now);
            usage.setReleasedAt(null);
            couponUsageRepository.save(usage);
        }
    }

    private Coupon resolveCoupon(User user, String couponCode, BigDecimal eligibleAmount, LocalDateTime now) {
        String normalized = normalizeCode(couponCode);
        if (normalized == null) {
            return null;
        }
        Coupon coupon = couponRepository.findByCode(normalized)
                .orElseThrow(() -> new AppException(CouponErrorCode.COUPON_NOT_FOUND));
        validateCouponAvailability(user, coupon, eligibleAmount, now);
        return coupon;
    }

    private void validateCouponAvailability(User user, Coupon coupon, BigDecimal eligibleAmount, LocalDateTime now) {
        if (!Boolean.TRUE.equals(coupon.getActive())) {
            throw new AppException(CouponErrorCode.COUPON_INVALID);
        }
        if (coupon.getStartAt() == null || coupon.getEndAt() == null || coupon.getStartAt().isAfter(now)
                || coupon.getEndAt().isBefore(now)) {
            throw new AppException(CouponErrorCode.COUPON_EXPIRED);
        }
        if (coupon.getMinOrderAmount() != null && eligibleAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new AppException(CouponErrorCode.COUPON_MIN_ORDER_NOT_MET);
        }
        if (coupon.getUsageLimit() != null && couponUsageRepository.countByCoupon_IdAndStatusIn(coupon.getId(),
                CONSUMING_STATUSES) >= coupon.getUsageLimit()) {
            throw new AppException(CouponErrorCode.COUPON_USAGE_LIMIT_EXCEEDED);
        }
        if (coupon.getUsageLimitPerUser() != null
                && couponUsageRepository.countByCoupon_IdAndUser_IdAndStatusIn(coupon.getId(), user.getId(),
                        CONSUMING_STATUSES) >= coupon.getUsageLimitPerUser()) {
            throw new AppException(CouponErrorCode.COUPON_USAGE_LIMIT_EXCEEDED);
        }
    }

    private BigDecimal couponDiscount(Coupon coupon, BigDecimal eligibleAmount) {
        BigDecimal discount = coupon.getType() == CouponType.PERCENT
                ? eligibleAmount.multiply(coupon.getValue()).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                : coupon.getValue();
        if (coupon.getMaxDiscountAmount() != null) {
            discount = discount.min(coupon.getMaxDiscountAmount());
        }
        return money(discount.min(eligibleAmount).max(BigDecimal.ZERO));
    }

    private BigDecimal promotionDiscount(Promotion promotion, BigDecimal lineSubtotal, int quantity) {
        BigDecimal discount = promotion.getPromotionType() == PromotionType.PERCENT
                ? lineSubtotal.multiply(promotion.getValue()).divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                : promotion.getValue().multiply(BigDecimal.valueOf(quantity));
        if (promotion.getMaxDiscountAmount() != null) {
            discount = discount.min(promotion.getMaxDiscountAmount());
        }
        return money(discount.min(lineSubtotal).max(BigDecimal.ZERO));
    }

    private boolean matches(Promotion promotion, Product product) {
        if (promotion.getPromotionScope() == PromotionScope.PRODUCT) {
            return Objects.equals(promotion.getTargetId(), product.getId());
        }
        return product.getCategory() != null && Objects.equals(promotion.getTargetId(), product.getCategory().getId());
    }

    private CheckoutPreviewItemResponse toPreviewItem(DiscountItem item) {
        ProductVariation variation = item.variation();
        Product product = variation.getProduct();
        return CheckoutPreviewItemResponse.builder().cartItemId(item.cartItem().getId()).productId(product.getId())
                .productVariationId(variation.getId()).productName(product.getProductName()).sku(variation.getSku())
                .quantity(item.cartItem().getQuantity()).unitPrice(item.unitPrice()).lineSubtotal(item.lineSubtotal())
                .promotionDiscountAmount(item.promotionDiscountAmount()).promotionName(item.promotionName())
                .finalLineAmount(item.finalLineAmount()).build();
    }

    private List<CouponUsage> reservedUsages(List<Order> orders) {
        List<Long> orderIds = orderIds(orders);
        return orderIds.isEmpty() ? List.of()
                : couponUsageRepository.findByOrder_IdInAndStatus(orderIds, CouponUsageStatus.RESERVED);
    }

    private List<Long> orderIds(List<Order> orders) {
        return orders == null ? List.of()
                : orders.stream().map(Order::getId).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private String normalizeCode(String couponCode) {
        return StringUtils.hasText(couponCode) ? couponCode.trim().toUpperCase(Locale.ROOT) : null;
    }

    private BigDecimal money(BigDecimal value) {
        return nullToZero(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }
}
