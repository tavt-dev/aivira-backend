package com.tien.aivirabackend.service.discount;

import java.math.BigDecimal;
import java.util.List;

import com.tien.aivirabackend.domain.entity.discount.Coupon;

public record DiscountCalculation(BigDecimal subtotal, BigDecimal promotionDiscountAmount,
        BigDecimal couponDiscountAmount, BigDecimal discountAmount, BigDecimal shippingFee, BigDecimal totalAmount,
        String couponCode, Coupon coupon, List<DiscountItem> items) {
}
