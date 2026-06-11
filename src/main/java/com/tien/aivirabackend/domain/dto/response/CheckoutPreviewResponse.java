package com.tien.aivirabackend.domain.dto.response;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(
        description =
                "Non-mutating checkout preview totals. Preview does not lock stock, create an order, or consume coupon usage.")
public class CheckoutPreviewResponse {
    @Schema(description = "Original cart subtotal before discounts.", example = "640000")
    BigDecimal subtotal;

    @Schema(description = "Total item-level promotion discount.", example = "50000")
    BigDecimal promotionDiscountAmount;

    @Schema(description = "Order-level coupon discount after promotions.", example = "30000")
    BigDecimal couponDiscountAmount;

    @Schema(description = "promotionDiscountAmount + couponDiscountAmount.", example = "80000")
    BigDecimal discountAmount;

    @Schema(example = "0")
    BigDecimal shippingFee;

    @Schema(description = "Payable amount after discounts and shipping.", example = "560000")
    BigDecimal totalAmount;

    @Schema(example = "AIVIRA10")
    String couponCode;

    List<CheckoutPreviewItemResponse> items;
    List<AppliedPromotionResponse> appliedPromotions;
    CouponResponse coupon;
}
