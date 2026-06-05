package com.tien.aivirabackend.domain.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CheckoutPreviewResponse {
    BigDecimal subtotal;
    BigDecimal promotionDiscountAmount;
    BigDecimal couponDiscountAmount;
    BigDecimal discountAmount;
    BigDecimal shippingFee;
    BigDecimal totalAmount;
    String couponCode;
    List<CheckoutPreviewItemResponse> items;
    List<AppliedPromotionResponse> appliedPromotions;
    CouponResponse coupon;
}
