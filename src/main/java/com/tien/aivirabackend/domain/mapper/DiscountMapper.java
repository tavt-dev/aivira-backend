package com.tien.aivirabackend.domain.mapper;

import org.springframework.stereotype.Component;

import com.tien.aivirabackend.domain.dto.response.CouponResponse;
import com.tien.aivirabackend.domain.dto.response.PromotionResponse;
import com.tien.aivirabackend.domain.entity.discount.Coupon;
import com.tien.aivirabackend.domain.entity.discount.Promotion;

@Component
public class DiscountMapper {
    public CouponResponse toCouponResponse(Coupon coupon) {
        if (coupon == null) {
            return null;
        }
        return CouponResponse.builder().id(coupon.getId()).code(coupon.getCode()).type(coupon.getType())
                .value(coupon.getValue()).maxDiscountAmount(coupon.getMaxDiscountAmount())
                .minOrderAmount(coupon.getMinOrderAmount()).usageLimit(coupon.getUsageLimit())
                .usageLimitPerUser(coupon.getUsageLimitPerUser()).usedCount(coupon.getUsedCount())
                .startAt(coupon.getStartAt()).endAt(coupon.getEndAt()).active(coupon.getActive())
                .createdAt(coupon.getCreatedAt()).updatedAt(coupon.getUpdatedAt()).build();
    }

    public PromotionResponse toPromotionResponse(Promotion promotion) {
        if (promotion == null) {
            return null;
        }
        return PromotionResponse.builder().id(promotion.getId()).promotionName(promotion.getPromotionName())
                .description(promotion.getDescription()).promotionType(promotion.getPromotionType())
                .value(promotion.getValue()).maxDiscountAmount(promotion.getMaxDiscountAmount())
                .promotionScope(promotion.getPromotionScope()).targetId(promotion.getTargetId())
                .startAt(promotion.getStartAt()).endAt(promotion.getEndAt()).active(promotion.getActive())
                .createdAt(promotion.getCreatedAt()).updatedAt(promotion.getUpdatedAt()).build();
    }
}
