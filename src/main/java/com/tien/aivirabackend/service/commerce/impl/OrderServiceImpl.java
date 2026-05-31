package com.tien.aivirabackend.service.commerce.impl;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.tien.aivirabackend.constant.OrderStatus;
import com.tien.aivirabackend.constant.PaymentStatus;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.OrderCancelRequest;
import com.tien.aivirabackend.domain.dto.response.OrderResponse;
import com.tien.aivirabackend.domain.dto.response.OrderSummaryResponse;
import com.tien.aivirabackend.domain.entity.transaction.Order;
import com.tien.aivirabackend.domain.entity.transaction.payment.Payment;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentAttempt;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentGroup;
import com.tien.aivirabackend.domain.mapper.CommerceMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.OrderErrorCode;
import com.tien.aivirabackend.repository.OrderRepository;
import com.tien.aivirabackend.repository.PaymentAttemptRepository;
import com.tien.aivirabackend.repository.PaymentGroupRepository;
import com.tien.aivirabackend.service.auth.CurrentUserService;
import com.tien.aivirabackend.service.commerce.OrderService;
import com.tien.aivirabackend.service.commerce.InventoryService;
import com.tien.aivirabackend.util.PageRequestUtils;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderServiceImpl implements OrderService {
    OrderRepository orderRepository;
    PaymentGroupRepository paymentGroupRepository;
    PaymentAttemptRepository paymentAttemptRepository;
    CurrentUserService currentUserService;
    CommerceMapper commerceMapper;
    InventoryService inventoryService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> getMyOrders(OrderStatus status, int page, int size) {
        String userId = currentUserService.getCurrentUserId();
        var pageable = PageRequestUtils.newestFirst(page, size);
        var orderPage = (status == null
                        ? orderRepository.findByUserId(userId, pageable)
                        : orderRepository.findByUserIdAndOrderStatus(userId, status, pageable))
                .map(commerceMapper::toOrderSummaryResponse);
        return PageResponse.from(orderPage);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(Long orderId) {
        String userId = currentUserService.getCurrentUserId();
        Order order = orderRepository
                .findDetailedByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));
        return commerceMapper.toOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelMyOrder(Long orderId, OrderCancelRequest request) {
        String userId = currentUserService.getCurrentUserId();
        Order order = orderRepository
                .findByIdAndUserIdForUpdate(orderId, userId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        PaymentGroup paymentGroup = resolvePaymentGroup(order);
        validateCancelable(order, paymentGroup);

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancelReason(trimToNull(request == null ? null : request.getReason()));
        inventoryService.restoreStockForOrders(List.of(order));
        order.getPayments().stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PENDING)
                .forEach(payment -> payment.setStatus(PaymentStatus.CANCELLED));

        if (paymentGroup != null
                && paymentGroup.getStatus() == PaymentStatus.PENDING
                && orderRepository.countByPaymentsPaymentGroupId(paymentGroup.getId()) == 1) {
            paymentGroup.setStatus(PaymentStatus.CANCELLED);
            paymentGroupRepository.save(paymentGroup);
            cancelLatestPendingAttempt(paymentGroup);
        }

        return commerceMapper.toOrderResponse(orderRepository.save(order));
    }

    private void validateCancelable(Order order, PaymentGroup paymentGroup) {
        if (order.getOrderStatus() == OrderStatus.PAID) {
            throw new AppException(OrderErrorCode.ORDER_CANCEL_REQUIRES_REFUND);
        }
        if (order.getOrderStatus() == OrderStatus.PENDING_PAYMENT) {
            if (paymentGroup == null || orderRepository.countByPaymentsPaymentGroupId(paymentGroup.getId()) != 1) {
                throw new AppException(OrderErrorCode.ORDER_SHARED_PAYMENT_GROUP_CANCEL_NOT_SUPPORTED);
            }
            return;
        }
        if (order.getOrderStatus() == OrderStatus.PENDING_CONFIRMATION) {
            return;
        }
        throw new AppException(OrderErrorCode.ORDER_CANCEL_NOT_ALLOWED);
    }

    private PaymentGroup resolvePaymentGroup(Order order) {
        return order.getPayments().stream()
                .map(Payment::getPaymentGroup)
                .filter(group -> group != null)
                .min(Comparator.comparing(PaymentGroup::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    private void cancelLatestPendingAttempt(PaymentGroup paymentGroup) {
        paymentAttemptRepository
                .findTopByPaymentGroupIdOrderByAttemptNoDesc(paymentGroup.getId())
                .filter(attempt -> attempt.getStatus() == PaymentStatus.PENDING)
                .ifPresent(this::cancelAttempt);
    }

    private void cancelAttempt(PaymentAttempt attempt) {
        attempt.setStatus(PaymentStatus.CANCELLED);
        attempt.setCompletedAt(Instant.now());
        paymentAttemptRepository.save(attempt);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
