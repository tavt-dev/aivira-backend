package com.tien.aivirabackend.domain.mapper;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.tien.aivirabackend.constant.ProductStatus;
import com.tien.aivirabackend.domain.dto.response.*;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.entity.catalog.ProductVariation;
import com.tien.aivirabackend.domain.entity.transaction.Cart;
import com.tien.aivirabackend.domain.entity.transaction.CartItem;
import com.tien.aivirabackend.domain.entity.transaction.Order;
import com.tien.aivirabackend.domain.entity.transaction.OrderItem;
import com.tien.aivirabackend.domain.entity.transaction.Refund;
import com.tien.aivirabackend.domain.entity.transaction.payment.Payment;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentGroup;
import com.tien.aivirabackend.domain.entity.user.Address;

@Component
public class CommerceMapper {
    public AddressResponse toAddressResponse(Address address) {
        if (address == null) {
            return null;
        }
        return AddressResponse.builder()
                .id(address.getId())
                .recipientName(address.getRecipientName())
                .phoneNumber(address.getPhoneNumber())
                .addressLine(address.getAddressLine())
                .ward(address.getWard())
                .district(address.getDistrict())
                .city(address.getCity())
                .defaultAddress(address.getDefaultAddress())
                .active(address.getActive())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }

    public CartResponse toCartResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .sorted(Comparator.comparing(CartItem::getId))
                .map(this::toCartItemResponse)
                .toList();
        BigDecimal subtotal = items.stream()
                .map(item -> item.getFinalPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalItems = items.stream().mapToInt(CartItemResponse::getQuantity).sum();
        return CartResponse.builder()
                .id(cart.getId())
                .items(items)
                .totalItems(totalItems)
                .subtotal(subtotal)
                .build();
    }

    public CartItemResponse toCartItemResponse(CartItem item) {
        ProductVariation variation = item.getProductVariation();
        Product product = variation.getProduct();
        BigDecimal finalPrice = product.getPrice().add(nullToZero(variation.getAdditionalPrice()));
        boolean available = Boolean.TRUE.equals(product.getActive())
                && ProductStatus.ACTIVE == product.getStatus()
                && Boolean.TRUE.equals(variation.getActive())
                && variation.getStockQuantity() >= item.getQuantity();
        return CartItemResponse.builder()
                .id(item.getId())
                .productId(product.getId())
                .productVariationId(variation.getId())
                .productName(product.getProductName())
                .productSlug(product.getSlug())
                .thumbnailUrl(resolveThumbnail(product, variation))
                .sku(variation.getSku())
                .color(variation.getColor())
                .size(variation.getSize())
                .basePrice(product.getPrice())
                .additionalPrice(nullToZero(variation.getAdditionalPrice()))
                .finalPrice(finalPrice)
                .quantity(item.getQuantity())
                .stockQuantity(variation.getStockQuantity())
                .available(available)
                .build();
    }

    public OrderResponse toOrderResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .sorted(Comparator.comparing(OrderItem::getId))
                .map(this::toOrderItemResponse)
                .toList();
        Payment payment = primaryPayment(order);
        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .notes(order.getNotes())
                .cancelReason(order.getCancelReason())
                .orderStatus(order.getOrderStatus())
                .paymentGroupCode(
                        payment == null ? null : payment.getPaymentGroup().getPaymentCode())
                .paymentMethod(payment == null ? null : payment.getMethod())
                .paymentStatus(payment == null ? null : payment.getStatus())
                .paidAt(payment == null ? null : payment.getPaidAt())
                .refund(toRefundResponse(order.getRefund()))
                .shippingRecipientName(order.getShippingRecipientName())
                .shippingPhoneNumber(order.getShippingPhoneNumber())
                .shippingAddressLine(order.getShippingAddressLine())
                .shippingWard(order.getShippingWard())
                .shippingDistrict(order.getShippingDistrict())
                .shippingCity(order.getShippingCity())
                .items(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    public RefundResponse toRefundResponse(Refund refund) {
        if (refund == null) {
            return null;
        }
        Order order = refund.getOrder();
        return RefundResponse.builder()
                .id(refund.getId())
                .refundCode(refund.getRefundCode())
                .orderId(order == null ? null : order.getId())
                .orderCode(order == null ? null : order.getOrderCode())
                .amount(refund.getAmount())
                .reason(refund.getReason())
                .note(refund.getNote())
                .status(refund.getStatus())
                .refundedBy(refund.getRefundedBy())
                .refundedAt(refund.getRefundedAt())
                .createdAt(refund.getCreatedAt())
                .updatedAt(refund.getUpdatedAt())
                .build();
    }

    public OrderSummaryResponse toOrderSummaryResponse(Order order, List<OrderItem> summaryItems) {
        Payment payment = primaryPayment(order);
        List<OrderItem> orderedItems = summaryItems == null
                ? List.of()
                : summaryItems.stream().sorted(Comparator.comparing(OrderItem::getId)).toList();
        int itemCount = orderedItems.stream()
                .map(OrderItem::getQuantity)
                .filter(quantity -> quantity != null)
                .mapToInt(Integer::intValue)
                .sum();
        OrderPreviewItemResponse previewItem = orderedItems.stream()
                .findFirst()
                .map(item -> OrderPreviewItemResponse.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .thumbnailUrl(item.getThumbnailUrl())
                        .build())
                .orElse(null);
        return OrderSummaryResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .totalAmount(order.getTotalAmount())
                .orderStatus(order.getOrderStatus())
                .cancelReason(order.getCancelReason())
                .paymentGroupCode(
                        payment == null ? null : payment.getPaymentGroup().getPaymentCode())
                .paymentMethod(payment == null ? null : payment.getMethod())
                .paymentStatus(payment == null ? null : payment.getStatus())
                .paidAt(payment == null ? null : payment.getPaidAt())
                .itemCount(itemCount)
                .previewItem(previewItem)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    public OrderItemResponse toOrderItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productVariationId(item.getProductVariationId())
                .productName(item.getProductName())
                .sku(item.getSku())
                .variationColor(item.getVariationColor())
                .variationSize(item.getVariationSize())
                .thumbnailUrl(item.getThumbnailUrl())
                .basePrice(item.getBasePrice())
                .additionalPrice(item.getAdditionalPrice())
                .discountAmount(item.getDiscountAmount())
                .finalPrice(item.getFinalPrice())
                .quantity(item.getQuantity())
                .build();
    }

    public PaymentResponse toPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .paymentGroupCode(payment.getPaymentGroup().getPaymentCode())
                .orderId(payment.getOrder().getId())
                .orderCode(payment.getOrder().getOrderCode())
                .method(payment.getMethod())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .transactionId(payment.getTransactionId())
                .paidAt(payment.getPaidAt())
                .build();
    }

    public PaymentGroupResponse toPaymentGroupResponse(PaymentGroup group, List<Order> orders) {
        return PaymentGroupResponse.builder()
                .paymentCode(group.getPaymentCode())
                .method(group.getMethod())
                .status(group.getStatus())
                .amount(group.getAmount())
                .providerTxnRef(group.getProviderTxnRef())
                .providerTransactionId(group.getProviderTransactionId())
                .paymentUrl(group.getPaymentUrl())
                .deeplink(group.getDeeplink())
                .qrCodeUrl(group.getQrCodeUrl())
                .expiresAt(group.getExpiresAt())
                .paidAt(group.getPaidAt())
                .payments(group.getPayments().stream()
                        .map(this::toPaymentResponse)
                        .toList())
                .orders(orders.stream().map(this::toOrderResponse).toList())
                .build();
    }

    private String resolveThumbnail(Product product, ProductVariation variation) {
        if (variation.getImageUrl() != null && !variation.getImageUrl().isBlank()) {
            return variation.getImageUrl();
        }
        return product.getThumbnailUrl();
    }

    private Payment primaryPayment(Order order) {
        return order.getPayments().stream()
                .min(Comparator.comparing(Payment::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
