package com.tien.aivirabackend.service.discount;

import java.util.List;
import java.util.Map;

import com.tien.aivirabackend.domain.dto.response.CheckoutPreviewResponse;
import com.tien.aivirabackend.domain.entity.catalog.ProductVariation;
import com.tien.aivirabackend.domain.entity.transaction.CartItem;
import com.tien.aivirabackend.domain.entity.transaction.Order;
import com.tien.aivirabackend.domain.entity.user.User;

public interface DiscountService {
    DiscountCalculation calculate(
            User user, List<CartItem> cartItems, Map<Long, ProductVariation> variations, String couponCode);

    CheckoutPreviewResponse toPreviewResponse(DiscountCalculation calculation);

    void reserveOrFinalizeCoupon(User user, Order order, DiscountCalculation calculation, boolean finalizeImmediately);

    void finalizeReservedCouponUsagesForOrders(List<Order> orders);

    void releaseReservedCouponUsagesForOrders(List<Order> orders);

    void reserveReleasedCouponUsagesForRetry(List<Order> orders);
}
