package com.tien.aivirabackend.service.commerce;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.tien.aivirabackend.constant.OrderStatus;
import com.tien.aivirabackend.constant.PaymentStatus;
import com.tien.aivirabackend.constant.RefundStatus;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.ManualRefundRequest;
import com.tien.aivirabackend.domain.dto.request.OrderCancelRequest;
import com.tien.aivirabackend.domain.dto.response.OrderResponse;
import com.tien.aivirabackend.domain.dto.response.OrderSummaryResponse;
import com.tien.aivirabackend.domain.entity.transaction.Order;
import com.tien.aivirabackend.domain.entity.transaction.Refund;
import com.tien.aivirabackend.domain.entity.transaction.payment.Payment;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentAttempt;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentGroup;
import com.tien.aivirabackend.domain.mapper.CommerceMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.OrderErrorCode;
import com.tien.aivirabackend.repository.OrderRepository;
import com.tien.aivirabackend.repository.OrderItemRepository;
import com.tien.aivirabackend.repository.PaymentAttemptRepository;
import com.tien.aivirabackend.repository.PaymentGroupRepository;
import com.tien.aivirabackend.repository.ProductRepository;
import com.tien.aivirabackend.repository.RefundRepository;
import com.tien.aivirabackend.service.auth.CurrentUserService;
import com.tien.aivirabackend.service.discount.DiscountService;
import com.tien.aivirabackend.service.notification.OrderNotificationAction;
import com.tien.aivirabackend.service.notification.OrderNotificationEvent;
import com.tien.aivirabackend.service.notification.OrderNotificationProducer;
import com.tien.aivirabackend.util.PageRequestUtils;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j(topic = "ORDER-SERVICE")
public class OrderServiceImpl implements OrderService {
    OrderRepository orderRepository;
    OrderItemRepository orderItemRepository;
    RefundRepository refundRepository;
    PaymentGroupRepository paymentGroupRepository;
    PaymentAttemptRepository paymentAttemptRepository;
    ProductRepository productRepository;
    CurrentUserService currentUserService;
    CommerceMapper commerceMapper;
    InventoryService inventoryService;
    OrderSpecifications orderSpecifications;
    DiscountService discountService;
    OrderNotificationProducer orderNotificationProducer;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> getMyOrders(OrderStatus status, int page, int size) {
        String userId = currentUserService.getCurrentUserId();
        var pageable = PageRequestUtils.newestFirst(page, size);
        Page<Order> orderPage = status == null ? orderRepository.findByUserId(userId, pageable)
                : orderRepository.findByUserIdAndOrderStatus(userId, status, pageable);
        return toOrderSummaryPage(orderPage);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(Long orderId) {
        String userId = currentUserService.getCurrentUserId();
        Order order = orderRepository.findWithItemsAndRefundByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));
        orderRepository.findWithPaymentsByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));
        return commerceMapper.toOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelMyOrder(Long orderId, OrderCancelRequest request) {
        String userId = currentUserService.getCurrentUserId();
        Order order = orderRepository.findByIdAndUserIdForUpdate(orderId, userId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

        PaymentGroup paymentGroup = resolvePaymentGroup(order);
        validateCancelable(order, paymentGroup);

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancelReason(trimToNull(request == null ? null : request.getReason()));
        inventoryService.restoreStockForOrders(List.of(order));
        discountService.releaseReservedCouponUsagesForOrders(List.of(order));
        order.getPayments().stream().filter(payment -> payment.getStatus() == PaymentStatus.PENDING)
                .forEach(payment -> payment.setStatus(PaymentStatus.CANCELLED));

        if (paymentGroup != null && paymentGroup.getStatus() == PaymentStatus.PENDING
                && orderRepository.countByPaymentsPaymentGroupId(paymentGroup.getId()) == 1) {
            paymentGroup.setStatus(PaymentStatus.CANCELLED);
            paymentGroupRepository.save(paymentGroup);
            cancelLatestPendingAttempt(paymentGroup);
        }

        return commerceMapper.toOrderResponse(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> getAdminOrders(OrderStatus status, PaymentStatus paymentStatus, String keyword, Instant fromDate,
            Instant toDate, int page, int size) {
        var pageable = PageRequestUtils.newestFirst(page, size);
        Specification<Order> specification = orderSpecifications.adminOrders(status, paymentStatus, keyword, fromDate, toDate);
        return toOrderSummaryPage(orderRepository.findAll(specification, pageable));
    }

    private PageResponse<OrderSummaryResponse> toOrderSummaryPage(Page<Order> orderPage) {
        if (orderPage.isEmpty()) {
            return PageResponse.from(orderPage.map(order -> commerceMapper.toOrderSummaryResponse(order, List.of())));
        }

        List<Long> orderIds = orderPage.getContent().stream().map(Order::getId).toList();
        Map<Long, List<com.tien.aivirabackend.domain.entity.transaction.OrderItem>> itemsByOrderId = orderItemRepository
                .findByOrderIdInOrderByOrderIdAscIdAsc(orderIds).stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getId()));
        return PageResponse.from(orderPage.map(order -> commerceMapper.toOrderSummaryResponse(order,
                itemsByOrderId.getOrDefault(order.getId(), Collections.emptyList()))));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getAdminOrder(Long orderId) {
        Order order = orderRepository.findWithItemsAndRefundById(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));
        orderRepository.findWithPaymentsById(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));
        return commerceMapper.toOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse confirmOrder(Long orderId) {
        return transitionAny(orderId, Set.of(OrderStatus.PENDING_CONFIRMATION, OrderStatus.PAID),
                OrderStatus.CONFIRMED);
    }

    @Override
    @Transactional
    public OrderResponse markPacking(Long orderId) {
        return transition(orderId, OrderStatus.CONFIRMED, OrderStatus.PACKING);
    }

    @Override
    @Transactional
    public OrderResponse markShipping(Long orderId) {
        return transition(orderId, OrderStatus.PACKING, OrderStatus.SHIPPING);
    }

    @Override
    @Transactional
    public OrderResponse markCompleted(Long orderId) {
        return transition(orderId, OrderStatus.SHIPPING, OrderStatus.COMPLETED);
    }

    @Override
    @Transactional
    public OrderResponse cancelAdminOrder(Long orderId, OrderCancelRequest request) {
        Order order = findOrderForAdminUpdate(orderId);
        validateAdminCancelable(order);
        OrderStatus previousStatus = order.getOrderStatus();
        restoreStockAndCancel(order, trimToNull(request == null ? null : request.getReason()));
        logAdminLifecycleChange(order, previousStatus, OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);
        notifyAdminStatusChange(savedOrder, previousStatus, OrderStatus.CANCELLED, savedOrder.getCancelReason());
        return commerceMapper.toOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse markRefunded(Long orderId, ManualRefundRequest request) {
        Order order = findOrderForAdminUpdate(orderId);
        validateRefundable(order);
        validateRefundAmount(order, request == null ? null : request.getAmount());
        OrderStatus previousStatus = order.getOrderStatus();

        Instant now = Instant.now();
        String adminUserId = resolveCurrentUserIdForLog();
        if (!StringUtils.hasText(adminUserId)) {
            adminUserId = "UNKNOWN";
        }
        Refund refund = Refund.builder().refundCode(generateRefundCode()).order(order).amount(request.getAmount())
                .reason(trimRequired(request.getReason())).note(trimRequired(request.getNote()))
                .status(RefundStatus.COMPLETED).refundedBy(adminUserId).refundedAt(now).build();

        order.setOrderStatus(OrderStatus.REFUNDED);
        successfulPayments(order).forEach(payment -> {
            payment.setStatus(PaymentStatus.REFUNDED);
            if (payment.getPaymentGroup() != null) {
                payment.getPaymentGroup().setStatus(PaymentStatus.REFUNDED);
            }
        });
        inventoryService.restoreStockForOrders(List.of(order));
        Refund savedRefund = refundRepository.save(refund);
        order.setRefund(savedRefund);
        Order savedOrder = orderRepository.save(order);

        log.info("admin_refund_marked orderId={} orderCode={} refundCode={} amount={} adminUserId={}",
                savedOrder.getId(), savedOrder.getOrderCode(), savedRefund.getRefundCode(), savedRefund.getAmount(),
                adminUserId);
        notifyAdminStatusChange(savedOrder, previousStatus, OrderStatus.REFUNDED, null);
        return commerceMapper.toOrderResponse(savedOrder);
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

    private OrderResponse transition(Long orderId, OrderStatus expected, OrderStatus target) {
        return transitionAny(orderId, Set.of(expected), target);
    }

    private OrderResponse transitionAny(Long orderId, Set<OrderStatus> allowedSources, OrderStatus target) {
        Order order = findOrderForAdminUpdate(orderId);
        OrderStatus previousStatus = order.getOrderStatus();
        if (!allowedSources.contains(previousStatus)) {
            throw new AppException(OrderErrorCode.ORDER_INVALID_STATUS_TRANSITION);
        }
        order.setOrderStatus(target);
        if (target == OrderStatus.COMPLETED) {
            order.getItems()
                    .forEach(item -> productRepository.incrementSoldCount(item.getProductId(), item.getQuantity()));
        }
        logAdminLifecycleChange(order, previousStatus, target);
        Order savedOrder = orderRepository.save(order);
        notifyAdminStatusChange(savedOrder, previousStatus, target, null);
        return commerceMapper.toOrderResponse(savedOrder);
    }

    private void notifyAdminStatusChange(Order order, OrderStatus previousStatus, OrderStatus currentStatus,
            String cancelReason) {
        orderNotificationProducer.adminStatusUpdated(new OrderNotificationEvent(UUID.randomUUID().toString(),
                order.getId(), order.getOrderCode(), order.getUser().getId(), previousStatus, currentStatus,
                OrderNotificationAction.ADMIN_STATUS_UPDATED, cancelReason, resolveCurrentUserIdForLog(),
                Instant.now()));
    }

    private Order findOrderForAdminUpdate(Long orderId) {
        return orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));
    }

    private void validateAdminCancelable(Order order) {
        if (order.getOrderStatus() == OrderStatus.PAID) {
            throw new AppException(OrderErrorCode.ORDER_CANCEL_REQUIRES_REFUND);
        }
        if (Set.of(OrderStatus.PENDING_CONFIRMATION, OrderStatus.CONFIRMED, OrderStatus.PACKING)
                .contains(order.getOrderStatus())) {
            return;
        }
        throw new AppException(OrderErrorCode.ORDER_CANCEL_NOT_ALLOWED);
    }

    private void validateRefundable(Order order) {
        if (order.getOrderStatus() == OrderStatus.REFUNDED || order.getRefund() != null
                || refundRepository.existsByOrder_Id(order.getId())) {
            throw new AppException(OrderErrorCode.ORDER_REFUND_ALREADY_PROCESSED);
        }
        if (!Set.of(OrderStatus.PAID, OrderStatus.CONFIRMED, OrderStatus.PACKING).contains(order.getOrderStatus())) {
            throw new AppException(OrderErrorCode.ORDER_REFUND_NOT_ALLOWED);
        }
        if (successfulPayments(order).isEmpty()) {
            throw new AppException(OrderErrorCode.ORDER_REFUND_NOT_ALLOWED);
        }
    }

    private void validateRefundAmount(Order order, BigDecimal requestedAmount) {
        if (requestedAmount == null || requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(OrderErrorCode.ORDER_REFUND_AMOUNT_INVALID);
        }
        BigDecimal successfulPaymentAmount = successfulPayments(order).stream().map(Payment::getAmount)
                .filter(amount -> amount != null).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (requestedAmount.compareTo(successfulPaymentAmount) != 0
                || requestedAmount.compareTo(order.getTotalAmount()) != 0) {
            throw new AppException(OrderErrorCode.ORDER_REFUND_AMOUNT_INVALID);
        }
    }

    private List<Payment> successfulPayments(Order order) {
        return order.getPayments().stream().filter(payment -> payment.getStatus() == PaymentStatus.SUCCESS).toList();
    }

    private void restoreStockAndCancel(Order order, String reason) {
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancelReason(reason);
        inventoryService.restoreStockForOrders(List.of(order));
        discountService.releaseReservedCouponUsagesForOrders(List.of(order));
        order.getPayments().stream().filter(payment -> payment.getStatus() == PaymentStatus.PENDING)
                .forEach(payment -> payment.setStatus(PaymentStatus.CANCELLED));
    }

    private void logAdminLifecycleChange(Order order, OrderStatus previousStatus, OrderStatus targetStatus) {
        log.info(
                "Admin order lifecycle change: orderId={} orderCode={} previousStatus={} targetStatus={} adminUserId={}",
                order.getId(), order.getOrderCode(), previousStatus, targetStatus, resolveCurrentUserIdForLog());
    }

    private String resolveCurrentUserIdForLog() {
        var userId = currentUserService.findCurrentUserId();
        return userId == null ? null : userId.orElse(null);
    }

    private PaymentGroup resolvePaymentGroup(Order order) {
        return order.getPayments().stream().map(Payment::getPaymentGroup).filter(group -> group != null)
                .min(Comparator.comparing(PaymentGroup::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    private void cancelLatestPendingAttempt(PaymentGroup paymentGroup) {
        paymentAttemptRepository.findTopByPaymentGroupIdOrderByAttemptNoDesc(paymentGroup.getId())
                .filter(attempt -> attempt.getStatus() == PaymentStatus.PENDING).ifPresent(this::cancelAttempt);
    }

    private void cancelAttempt(PaymentAttempt attempt) {
        attempt.setStatus(PaymentStatus.CANCELLED);
        attempt.setCompletedAt(Instant.now());
        paymentAttemptRepository.save(attempt);
    }

    private String generateRefundCode() {
        String code;
        do {
            code = "REF" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (refundRepository.existsByRefundCode(code));
        return code;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String trimRequired(String value) {
        if (!StringUtils.hasText(value)) {
            throw new AppException(OrderErrorCode.ORDER_REFUND_NOT_ALLOWED);
        }
        return value.trim();
    }
}
