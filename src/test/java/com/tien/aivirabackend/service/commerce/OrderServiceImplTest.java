package com.tien.aivirabackend.service.commerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.tien.aivirabackend.constant.OrderStatus;
import com.tien.aivirabackend.constant.PaymentMethod;
import com.tien.aivirabackend.constant.PaymentProvider;
import com.tien.aivirabackend.constant.PaymentStatus;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.ManualRefundRequest;
import com.tien.aivirabackend.domain.dto.request.OrderCancelRequest;
import com.tien.aivirabackend.domain.dto.response.OrderSummaryResponse;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.entity.catalog.ProductVariation;
import com.tien.aivirabackend.domain.entity.transaction.Order;
import com.tien.aivirabackend.domain.entity.transaction.OrderItem;
import com.tien.aivirabackend.domain.entity.transaction.Refund;
import com.tien.aivirabackend.domain.entity.transaction.payment.Payment;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentAttempt;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentGroup;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.mapper.CommerceMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.OrderErrorCode;
import com.tien.aivirabackend.repository.OrderRepository;
import com.tien.aivirabackend.repository.OrderItemRepository;
import com.tien.aivirabackend.repository.PaymentAttemptRepository;
import com.tien.aivirabackend.repository.PaymentGroupRepository;
import com.tien.aivirabackend.repository.ProductRepository;
import com.tien.aivirabackend.repository.ProductVariationRepository;
import com.tien.aivirabackend.repository.RefundRepository;
import com.tien.aivirabackend.service.auth.CurrentUserService;
import com.tien.aivirabackend.service.discount.DiscountService;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {
    @Mock
    OrderRepository orderRepository;

    @Mock
    OrderItemRepository orderItemRepository;

    @Mock
    RefundRepository refundRepository;

    @Mock
    PaymentGroupRepository paymentGroupRepository;

    @Mock
    PaymentAttemptRepository paymentAttemptRepository;

    @Mock
    ProductRepository productRepository;

    @Mock
    ProductVariationRepository variationRepository;

    @Mock
    CurrentUserService currentUserService;

    @Mock
    DiscountService discountService;

    OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
                orderRepository,
                orderItemRepository,
                refundRepository,
                paymentGroupRepository,
                paymentAttemptRepository,
                productRepository,
                currentUserService,
                new CommerceMapper(),
                new InventoryService(variationRepository),
                new OrderSpecifications(),
                discountService);
    }

    @Test
    void getMyOrders_whenStatusMissing_shouldQueryCurrentUserNewestFirst() {
        Order order = order(paymentGroup(PaymentMethod.COD, PaymentStatus.PENDING), OrderStatus.PENDING_CONFIRMATION);
        when(currentUserService.getCurrentUserId()).thenReturn("user-1");
        when(orderRepository.findByUserId(eq("user-1"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(order)));
        when(orderItemRepository.findByOrderIdInOrderByOrderIdAscIdAsc(List.of(21L)))
                .thenReturn(order.getItems());

        PageResponse<OrderSummaryResponse> response = orderService.getMyOrders(null, 1, 20);

        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().getFirst().getOrderCode()).isEqualTo("ORD123");
        assertThat(response.getData().getFirst().getItemCount()).isEqualTo(2);
        assertThat(response.getData().getFirst().getPreviewItem().getProductName()).isEqualTo("Dress");
        assertThat(response.getData().getFirst().getPreviewItem().getProductId()).isEqualTo(10L);
        verify(orderRepository)
                .findByUserId(
                        eq("user-1"),
                        argThat(pageable -> pageable.getPageNumber() == 0
                                && pageable.getPageSize() == 20
                                && pageable.getSort().getOrderFor("createdAt") != null));
        verify(orderRepository, never()).findByUserIdAndOrderStatus(anyString(), any(), any());
    }

    @Test
    void getMyOrders_whenStatusProvided_shouldFilterByCurrentUserAndStatus() {
        when(currentUserService.getCurrentUserId()).thenReturn("user-1");
        when(orderRepository.findByUserIdAndOrderStatus(
                        eq("user-1"), eq(OrderStatus.CANCELLED), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        orderService.getMyOrders(OrderStatus.CANCELLED, 2, 10);

        verify(orderRepository)
                .findByUserIdAndOrderStatus(
                        eq("user-1"), eq(OrderStatus.CANCELLED), argThat(pageable -> pageable.getPageNumber() == 1));
        verify(orderRepository, never()).findByUserId(anyString(), any());
        verify(orderItemRepository, never()).findByOrderIdInOrderByOrderIdAscIdAsc(any());
    }

    @Test
    void getMyOrder_whenOwnedOrderExists_shouldReturnDetail() {
        Order order = order(paymentGroup(PaymentMethod.COD, PaymentStatus.PENDING), OrderStatus.PENDING_CONFIRMATION);
        when(currentUserService.getCurrentUserId()).thenReturn("user-1");
        when(orderRepository.findWithItemsAndRefundByIdAndUserId(21L, "user-1"))
                .thenReturn(Optional.of(order));
        when(orderRepository.findWithPaymentsByIdAndUserId(21L, "user-1")).thenReturn(Optional.of(order));

        var response = orderService.getMyOrder(21L);

        assertThat(response.getId()).isEqualTo(21L);
        assertThat(response.getPaymentGroupCode()).isEqualTo("PAY123");
        assertThat(response.getItems()).hasSize(1);
        verify(orderRepository).findWithItemsAndRefundByIdAndUserId(21L, "user-1");
        verify(orderRepository).findWithPaymentsByIdAndUserId(21L, "user-1");
    }

    @Test
    void getMyOrder_whenOrderDoesNotBelongToCurrentUser_shouldThrowNotFound() {
        when(currentUserService.getCurrentUserId()).thenReturn("user-1");
        when(orderRepository.findWithItemsAndRefundByIdAndUserId(99L, "user-1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getMyOrder(99L))
                .isInstanceOfSatisfying(AppException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(OrderErrorCode.ORDER_NOT_FOUND));
        verify(orderRepository, never()).findWithPaymentsByIdAndUserId(anyLong(), anyString());
    }

    @Test
    void getAdminOrders_shouldQueryNewestFirstWithFiltersAndMapSummaries() {
        Order order = order(paymentGroup(PaymentMethod.COD, PaymentStatus.PENDING), OrderStatus.PENDING_CONFIRMATION);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));

        PageResponse<OrderSummaryResponse> response = orderService.getAdminOrders(
                OrderStatus.PENDING_CONFIRMATION,
                "ORD123",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-12-31T23:59:59Z"),
                1,
                20);

        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().getFirst().getOrderCode()).isEqualTo("ORD123");
        verify(orderRepository)
                .findAll(
                        any(Specification.class),
                        ArgumentMatchers.<Pageable>argThat(pageable -> pageable.getPageNumber() == 0
                                && pageable.getPageSize() == 20
                                && pageable.getSort().getOrderFor("createdAt") != null));
    }

    @Test
    void getAdminOrder_whenOrderExists_shouldReturnDetail() {
        Order order = order(paymentGroup(PaymentMethod.COD, PaymentStatus.PENDING), OrderStatus.PENDING_CONFIRMATION);
        when(orderRepository.findWithItemsAndRefundById(21L)).thenReturn(Optional.of(order));
        when(orderRepository.findWithPaymentsById(21L)).thenReturn(Optional.of(order));

        var response = orderService.getAdminOrder(21L);

        assertThat(response.getId()).isEqualTo(21L);
        assertThat(response.getPaymentGroupCode()).isEqualTo("PAY123");
        verify(orderRepository).findWithItemsAndRefundById(21L);
        verify(orderRepository).findWithPaymentsById(21L);
    }

    @Test
    void getAdminOrder_whenMissing_shouldThrowNotFound() {
        when(orderRepository.findWithItemsAndRefundById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getAdminOrder(99L))
                .isInstanceOfSatisfying(AppException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(OrderErrorCode.ORDER_NOT_FOUND));
        verify(orderRepository, never()).findWithPaymentsById(anyLong());
    }

    @Test
    void cancelMyOrder_whenPendingConfirmation_shouldCancelPaymentAndRestoreStock() {
        PaymentGroup group = paymentGroup(PaymentMethod.COD, PaymentStatus.PENDING);
        Order order = order(group, OrderStatus.PENDING_CONFIRMATION);
        ProductVariation variation = variation(5);
        when(currentUserService.getCurrentUserId()).thenReturn("user-1");
        when(orderRepository.findByIdAndUserIdForUpdate(21L, "user-1")).thenReturn(Optional.of(order));
        when(orderRepository.countByPaymentsPaymentGroupId(group.getId())).thenReturn(1L);
        when(variationRepository.findAllByIdInForUpdate(anyCollection())).thenReturn(List.of(variation));
        when(paymentAttemptRepository.findTopByPaymentGroupIdOrderByAttemptNoDesc(group.getId()))
                .thenReturn(Optional.empty());
        when(orderRepository.save(order)).thenReturn(order);

        var response = orderService.cancelMyOrder(
                21L, OrderCancelRequest.builder().reason("  wrong address  ").build());

        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(response.getCancelReason()).isEqualTo("wrong address");
        assertThat(order.getPayments().getFirst().getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(group.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(variation.getStockQuantity()).isEqualTo(7);
        verify(paymentGroupRepository).save(group);
    }

    @Test
    void cancelMyOrder_whenPendingPaymentSingleOrderGroup_shouldCancelGroupAndLatestAttempt() {
        PaymentGroup group = paymentGroup(PaymentMethod.VNPAY, PaymentStatus.PENDING);
        PaymentAttempt attempt = attempt(group, PaymentStatus.PENDING);
        Order order = order(group, OrderStatus.PENDING_PAYMENT);
        ProductVariation variation = variation(3);
        when(currentUserService.getCurrentUserId()).thenReturn("user-1");
        when(orderRepository.findByIdAndUserIdForUpdate(21L, "user-1")).thenReturn(Optional.of(order));
        when(orderRepository.countByPaymentsPaymentGroupId(group.getId())).thenReturn(1L);
        when(variationRepository.findAllByIdInForUpdate(anyCollection())).thenReturn(List.of(variation));
        when(paymentAttemptRepository.findTopByPaymentGroupIdOrderByAttemptNoDesc(group.getId()))
                .thenReturn(Optional.of(attempt));
        when(orderRepository.save(order)).thenReturn(order);

        orderService.cancelMyOrder(21L, OrderCancelRequest.builder().build());

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(group.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(attempt.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(attempt.getCompletedAt()).isNotNull();
        assertThat(variation.getStockQuantity()).isEqualTo(5);
        verify(paymentAttemptRepository).save(attempt);
    }

    @Test
    void cancelMyOrder_whenPendingPaymentSharedGroup_shouldReject() {
        PaymentGroup group = paymentGroup(PaymentMethod.VNPAY, PaymentStatus.PENDING);
        Order order = order(group, OrderStatus.PENDING_PAYMENT);
        when(currentUserService.getCurrentUserId()).thenReturn("user-1");
        when(orderRepository.findByIdAndUserIdForUpdate(21L, "user-1")).thenReturn(Optional.of(order));
        when(orderRepository.countByPaymentsPaymentGroupId(group.getId())).thenReturn(2L);

        assertThatThrownBy(() -> orderService.cancelMyOrder(
                        21L, OrderCancelRequest.builder().build()))
                .isInstanceOfSatisfying(AppException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(OrderErrorCode.ORDER_SHARED_PAYMENT_GROUP_CANCEL_NOT_SUPPORTED));

        verify(variationRepository, never()).findAllByIdInForUpdate(anyCollection());
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void cancelMyOrder_whenPaid_shouldRequireRefund() {
        Order order = order(paymentGroup(PaymentMethod.VNPAY, PaymentStatus.SUCCESS), OrderStatus.PAID);
        when(currentUserService.getCurrentUserId()).thenReturn("user-1");
        when(orderRepository.findByIdAndUserIdForUpdate(21L, "user-1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelMyOrder(
                        21L, OrderCancelRequest.builder().build()))
                .isInstanceOfSatisfying(AppException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(OrderErrorCode.ORDER_CANCEL_REQUIRES_REFUND));
    }

    @Test
    void cancelMyOrder_whenTerminalOrFulfillmentStatus_shouldRejectWithoutRestoringStock() {
        List<OrderStatus> rejectedStatuses = List.of(
                OrderStatus.CONFIRMED,
                OrderStatus.PACKING,
                OrderStatus.SHIPPING,
                OrderStatus.COMPLETED,
                OrderStatus.CANCELLED,
                OrderStatus.PAYMENT_FAILED,
                OrderStatus.EXPIRED,
                OrderStatus.REFUNDED);

        for (OrderStatus status : rejectedStatuses) {
            reset(orderRepository, variationRepository, currentUserService);
            Order order = order(paymentGroup(PaymentMethod.COD, PaymentStatus.PENDING), status);
            when(currentUserService.getCurrentUserId()).thenReturn("user-1");
            when(orderRepository.findByIdAndUserIdForUpdate(21L, "user-1")).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelMyOrder(
                            21L, OrderCancelRequest.builder().build()))
                    .as("status %s", status)
                    .isInstanceOfSatisfying(AppException.class, ex -> assertThat(ex.getErrorCode())
                            .isEqualTo(OrderErrorCode.ORDER_CANCEL_NOT_ALLOWED));

            verify(variationRepository, never()).findAllByIdInForUpdate(anyCollection());
            assertThat(order.getOrderStatus()).isEqualTo(status);
        }
    }

    @Test
    void confirmOrder_whenPendingConfirmation_shouldMoveToConfirmed() {
        Order order = order(paymentGroup(PaymentMethod.COD, PaymentStatus.PENDING), OrderStatus.PENDING_CONFIRMATION);
        when(orderRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        var response = orderService.confirmOrder(21L);

        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void confirmOrder_whenPaid_shouldMoveToConfirmed() {
        Order order = order(paymentGroup(PaymentMethod.VNPAY, PaymentStatus.SUCCESS), OrderStatus.PAID);
        when(orderRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        var response = orderService.confirmOrder(21L);

        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void fulfillmentTransitions_shouldMoveThroughExpectedFlow() {
        assertTransition(OrderStatus.CONFIRMED, OrderStatus.PACKING, () -> orderService.markPacking(21L));
        assertTransition(OrderStatus.PACKING, OrderStatus.SHIPPING, () -> orderService.markShipping(21L));
        assertTransition(OrderStatus.SHIPPING, OrderStatus.COMPLETED, () -> orderService.markCompleted(21L));
        verify(productRepository).incrementSoldCount(10L, 2);
    }

    @Test
    void markCompleted_whenPacking_shouldThrowInvalidTransition() {
        Order order = order(paymentGroup(PaymentMethod.COD, PaymentStatus.PENDING), OrderStatus.PACKING);
        when(orderRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.markCompleted(21L))
                .isInstanceOfSatisfying(AppException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(OrderErrorCode.ORDER_INVALID_STATUS_TRANSITION));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelAdminOrder_whenPendingConfirmation_shouldRestoreStockCancelPendingPaymentAndSetReason() {
        Order order = order(paymentGroup(PaymentMethod.COD, PaymentStatus.PENDING), OrderStatus.PENDING_CONFIRMATION);
        ProductVariation variation = variation(5);
        when(orderRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(order));
        when(variationRepository.findAllByIdInForUpdate(anyCollection())).thenReturn(List.of(variation));
        when(orderRepository.save(order)).thenReturn(order);

        var response = orderService.cancelAdminOrder(
                21L, OrderCancelRequest.builder().reason("  out of stock  ").build());

        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(response.getCancelReason()).isEqualTo("out of stock");
        assertThat(order.getPayments().getFirst().getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(variation.getStockQuantity()).isEqualTo(7);
        verify(paymentGroupRepository, never()).save(any());
        verify(paymentAttemptRepository, never()).save(any());
    }

    @Test
    void cancelAdminOrder_whenConfirmedOrPacking_shouldRestoreStock() {
        for (OrderStatus status : List.of(OrderStatus.CONFIRMED, OrderStatus.PACKING)) {
            reset(orderRepository, variationRepository, paymentGroupRepository, paymentAttemptRepository);
            Order order = order(paymentGroup(PaymentMethod.COD, PaymentStatus.PENDING), status);
            ProductVariation variation = variation(1);
            when(orderRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(order));
            when(variationRepository.findAllByIdInForUpdate(anyCollection())).thenReturn(List.of(variation));
            when(orderRepository.save(order)).thenReturn(order);

            orderService.cancelAdminOrder(21L, OrderCancelRequest.builder().build());

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(variation.getStockQuantity()).isEqualTo(3);
        }
    }

    @Test
    void cancelAdminOrder_whenPaid_shouldRequireRefund() {
        Order order = order(paymentGroup(PaymentMethod.VNPAY, PaymentStatus.SUCCESS), OrderStatus.PAID);
        when(orderRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelAdminOrder(
                        21L, OrderCancelRequest.builder().build()))
                .isInstanceOfSatisfying(AppException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(OrderErrorCode.ORDER_CANCEL_REQUIRES_REFUND));

        verify(variationRepository, never()).findAllByIdInForUpdate(anyCollection());
    }

    @Test
    void cancelAdminOrder_whenShippingOrCompleted_shouldReject() {
        for (OrderStatus status : List.of(OrderStatus.SHIPPING, OrderStatus.COMPLETED)) {
            reset(orderRepository, variationRepository);
            Order order = order(paymentGroup(PaymentMethod.COD, PaymentStatus.PENDING), status);
            when(orderRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.cancelAdminOrder(
                            21L, OrderCancelRequest.builder().build()))
                    .as("status %s", status)
                    .isInstanceOfSatisfying(AppException.class, ex -> assertThat(ex.getErrorCode())
                            .isEqualTo(OrderErrorCode.ORDER_CANCEL_NOT_ALLOWED));

            verify(variationRepository, never()).findAllByIdInForUpdate(anyCollection());
            assertThat(order.getOrderStatus()).isEqualTo(status);
        }
    }

    @Test
    void markRefunded_whenPaidOrder_shouldCreateRefundRestoreStockAndMarkStatuses() {
        PaymentGroup group = paymentGroup(PaymentMethod.VNPAY, PaymentStatus.SUCCESS);
        Order order = order(group, OrderStatus.PAID);
        ProductVariation variation = variation(5);
        when(orderRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(order));
        when(currentUserService.findCurrentUserId()).thenReturn(Optional.of("admin-1"));
        when(variationRepository.findAllByIdInForUpdate(anyCollection())).thenReturn(List.of(variation));
        when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> {
            Refund refund = invocation.getArgument(0);
            refund.setId(91L);
            return refund;
        });
        when(orderRepository.save(order)).thenReturn(order);

        var response = orderService.markRefunded(21L, refundRequest("100.00"));

        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(response.getRefund()).isNotNull();
        assertThat(response.getRefund().getRefundedBy()).isEqualTo("admin-1");
        assertThat(response.getRefund().getAmount()).isEqualByComparingTo("100.00");
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(order.getPayments().getFirst().getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(group.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(variation.getStockQuantity()).isEqualTo(7);

        ArgumentCaptor<Refund> refundCaptor = ArgumentCaptor.forClass(Refund.class);
        verify(refundRepository).save(refundCaptor.capture());
        assertThat(refundCaptor.getValue().getRefundCode()).startsWith("REF");
        assertThat(refundCaptor.getValue().getReason()).isEqualTo("Customer refund");
        assertThat(refundCaptor.getValue().getNote()).isEqualTo("Manual bank transfer completed");
        assertThat(refundCaptor.getValue().getStatus().name()).isEqualTo("COMPLETED");
        assertThat(refundCaptor.getValue().getRefundedAt()).isNotNull();
    }

    @Test
    void markRefunded_whenConfirmedOrPackingPaidOrder_shouldRestoreStock() {
        for (OrderStatus status : List.of(OrderStatus.CONFIRMED, OrderStatus.PACKING)) {
            reset(orderRepository, refundRepository, variationRepository, currentUserService);
            PaymentGroup group = paymentGroup(PaymentMethod.VNPAY, PaymentStatus.SUCCESS);
            Order order = order(group, status);
            ProductVariation variation = variation(1);
            when(orderRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(order));
            when(currentUserService.findCurrentUserId()).thenReturn(Optional.of("admin-1"));
            when(variationRepository.findAllByIdInForUpdate(anyCollection())).thenReturn(List.of(variation));
            when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(orderRepository.save(order)).thenReturn(order);

            orderService.markRefunded(21L, refundRequest("100.00"));

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.REFUNDED);
            assertThat(group.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(variation.getStockQuantity()).isEqualTo(3);
        }
    }

    @Test
    void markRefunded_whenUnpaidOrder_shouldReject() {
        Order order = order(paymentGroup(PaymentMethod.COD, PaymentStatus.PENDING), OrderStatus.PENDING_CONFIRMATION);
        when(orderRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.markRefunded(21L, refundRequest("100.00")))
                .isInstanceOfSatisfying(AppException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(OrderErrorCode.ORDER_REFUND_NOT_ALLOWED));

        verify(refundRepository, never()).save(any());
        verify(variationRepository, never()).findAllByIdInForUpdate(anyCollection());
    }

    @Test
    void markRefunded_whenShippingOrCompleted_shouldReject() {
        for (OrderStatus status : List.of(OrderStatus.SHIPPING, OrderStatus.COMPLETED)) {
            reset(orderRepository, refundRepository, variationRepository);
            Order order = order(paymentGroup(PaymentMethod.VNPAY, PaymentStatus.SUCCESS), status);
            when(orderRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.markRefunded(21L, refundRequest("100.00")))
                    .as("status %s", status)
                    .isInstanceOfSatisfying(AppException.class, ex -> assertThat(ex.getErrorCode())
                            .isEqualTo(OrderErrorCode.ORDER_REFUND_NOT_ALLOWED));

            verify(refundRepository, never()).save(any());
            verify(variationRepository, never()).findAllByIdInForUpdate(anyCollection());
        }
    }

    @Test
    void markRefunded_whenAlreadyRefunded_shouldReject() {
        Order order = order(paymentGroup(PaymentMethod.VNPAY, PaymentStatus.REFUNDED), OrderStatus.REFUNDED);
        when(orderRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.markRefunded(21L, refundRequest("100.00")))
                .isInstanceOfSatisfying(AppException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(OrderErrorCode.ORDER_REFUND_ALREADY_PROCESSED));

        verify(refundRepository, never()).save(any());
    }

    @Test
    void markRefunded_whenRefundRecordAlreadyExists_shouldReject() {
        Order order = order(paymentGroup(PaymentMethod.VNPAY, PaymentStatus.SUCCESS), OrderStatus.PAID);
        when(orderRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(order));
        when(refundRepository.existsByOrder_Id(21L)).thenReturn(true);

        assertThatThrownBy(() -> orderService.markRefunded(21L, refundRequest("100.00")))
                .isInstanceOfSatisfying(AppException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(OrderErrorCode.ORDER_REFUND_ALREADY_PROCESSED));

        verify(refundRepository, never()).save(any());
    }

    @Test
    void markRefunded_whenAmountMismatch_shouldReject() {
        Order order = order(paymentGroup(PaymentMethod.VNPAY, PaymentStatus.SUCCESS), OrderStatus.PAID);
        when(orderRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.markRefunded(21L, refundRequest("99.99")))
                .isInstanceOfSatisfying(AppException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(OrderErrorCode.ORDER_REFUND_AMOUNT_INVALID));

        verify(refundRepository, never()).save(any());
        verify(variationRepository, never()).findAllByIdInForUpdate(anyCollection());
    }

    private PaymentGroup paymentGroup(PaymentMethod method, PaymentStatus status) {
        PaymentGroup group = PaymentGroup.builder()
                .paymentCode("PAY123")
                .user(user())
                .method(method)
                .status(status)
                .amount(new BigDecimal("100.00"))
                .build();
        group.setId(1L);
        return group;
    }

    private PaymentAttempt attempt(PaymentGroup group, PaymentStatus status) {
        PaymentAttempt attempt = PaymentAttempt.builder()
                .paymentGroup(group)
                .provider(PaymentProvider.valueOf(group.getMethod().name()))
                .method(group.getMethod())
                .attemptNo(1)
                .providerTxnRef("PAY123-A1")
                .status(status)
                .amount(group.getAmount())
                .build();
        attempt.setId(11L);
        return attempt;
    }

    private Order order(PaymentGroup group, OrderStatus status) {
        Order order = Order.builder()
                .orderCode("ORD123")
                .user(group.getUser())
                .subtotal(new BigDecimal("100.00"))
                .totalAmount(new BigDecimal("100.00"))
                .orderStatus(status)
                .shippingRecipientName("Buyer")
                .shippingPhoneNumber("0900000000")
                .shippingAddressLine("123 Street")
                .build();
        order.setId(21L);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        Payment payment = Payment.builder()
                .order(order)
                .paymentGroup(group)
                .method(group.getMethod())
                .status(group.getStatus())
                .amount(group.getAmount())
                .build();
        payment.setId(31L);
        order.getPayments().add(payment);
        group.getPayments().add(payment);
        OrderItem item = OrderItem.builder()
                        .order(order)
                        .productId(10L)
                        .productVariationId(41L)
                        .productName("Dress")
                        .sku("DRESS-001")
                        .basePrice(new BigDecimal("50.00"))
                        .additionalPrice(BigDecimal.ZERO)
                        .finalPrice(new BigDecimal("50.00"))
                        .quantity(2)
                        .build();
        item.setId(51L);
        order.getItems().add(item);
        return order;
    }

    private ProductVariation variation(int stock) {
        Product product = Product.builder().stockQuantity(stock).build();
        ProductVariation variation =
                ProductVariation.builder().product(product).stockQuantity(stock).build();
        variation.setId(41L);
        return variation;
    }

    private User user() {
        return User.builder().id("user-1").username("buyer").build();
    }

    private ManualRefundRequest refundRequest(String amount) {
        return ManualRefundRequest.builder()
                .amount(new BigDecimal(amount))
                .reason("  Customer refund  ")
                .note("  Manual bank transfer completed  ")
                .build();
    }

    private void assertTransition(OrderStatus source, OrderStatus target, java.util.function.Supplier<?> action) {
        reset(orderRepository, productRepository);
        Order order = order(paymentGroup(PaymentMethod.COD, PaymentStatus.PENDING), source);
        when(orderRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        action.get();

        assertThat(order.getOrderStatus()).isEqualTo(target);
    }
}
