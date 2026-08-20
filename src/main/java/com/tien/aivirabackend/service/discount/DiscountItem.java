package com.tien.aivirabackend.service.discount;

import java.math.BigDecimal;

import com.tien.aivirabackend.domain.entity.catalog.ProductVariation;
import com.tien.aivirabackend.domain.entity.transaction.CartItem;

public record DiscountItem(CartItem cartItem, ProductVariation variation, BigDecimal unitPrice, BigDecimal lineSubtotal,
        BigDecimal promotionDiscountAmount, Long promotionId, String promotionName, BigDecimal finalLineAmount) {
}
