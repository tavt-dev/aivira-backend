package com.tien.aivirabackend.service.impl;

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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.tien.aivirabackend.constant.OrderStatus;
import com.tien.aivirabackend.constant.PaymentMethod;
import com.tien.aivirabackend.constant.PaymentProvider;
import com.tien.aivirabackend.constant.PaymentStatus;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.OrderCancelRequest;
import com.tien.aivirabackend.domain.dto.response.OrderSummaryResponse;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.entity.catalog.ProductVariation;
import com.tien.aivirabackend.domain.entity.marketplace.Shop;
import com.tien.aivirabackend.domain.entity.transaction.Order;
import com.tien.aivirabackend.domain.entity.transaction.OrderItem;
import com.tien.aivirabackend.domain.entity.transaction.payment.Payment;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentAttempt;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentGroup;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.mapper.CommerceMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.OrderErrorCode;
import com.tien.aivirabackend.repository.OrderRepository;
import com.tien.aivirabackend.repository.PaymentAttemptRepository;
import com.tien.aivirabackend.repository.PaymentGroupRepository;
import com.tien.aivirabackend.repository.ProductVariationRepository;
import com.tien.aivirabackend.service.CurrentUserService;
import com.tien.aivirabackend.service.commerce.InventoryService;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {
    @Mock
    OrderRepository orderRepository;

    @Mock
    PaymentGroupRepository paymentGroupRepository;

    @Mock
    PaymentAttemptRepository paymentAttemptRepository;

    @Mock
    ProductVariationRepository variationRepository;

    @Mock
    CurrentUserService currentUserService;

    OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
                orderRepository,
                paymentGroupRepository,
                paymentAttemptRepository,
                currentUserService,
                new CommerceMapper(),
                new InventoryService(variationRepository));
    }

    @Test
    void getMyOrders_whenStatusMissing_shouldQueryCurrentUserNewestFirst() {
        Order order = order(paymentGroup(PaymentMethod.COD, PaymentStatus.PENDING), OrderStatus.PENDING_CONFIRMATION);
        when(currentUserService.getCurrentUserId()).thenReturn("user-1");
        when(orderRepository.findByUserId(eq("user-1"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(order)));

        PageResponse<OrderSummaryResponse> response = orderService.getMyOrders(null, 1, 20);

        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().getFirst().getOrderCode()).isEqualTo("ORD123");
        assertThat(response.getData().getFirst().getItemCount()).isEqualTo(2);
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
    }

    @Test
    void getMyOrder_whenOwnedOrderExists_shouldReturnDetail() {
        Order order = order(paymentGroup(PaymentMethod.COD, PaymentStatus.PENDING), OrderStatus.PENDING_CONFIRMATION);
        when(currentUserService.getCurrentUserId()).thenReturn("user-1");
        when(orderRepository.findDetailedByIdAndUserId(21L, "user-1")).thenReturn(Optional.of(order));

        var response = orderService.getMyOrder(21L);

        assertThat(response.getId()).isEqualTo(21L);
        assertThat(response.getPaymentGroupCode()).isEqualTo("PAY123");
        assertThat(response.getItems()).hasSize(1);
    }

    @Test
    void getMyOrder_whenOrderDoesNotBelongToCurrentUser_shouldThrowNotFound() {
        when(currentUserService.getCurrentUserId()).thenReturn("user-1");
        when(orderRepository.findDetailedByIdAndUserId(99L, "user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getMyOrder(99L))
                .isInstanceOfSatisfying(AppException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(OrderErrorCode.ORDER_NOT_FOUND));
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
        Shop shop = Shop.builder().shopName("Aivira Shop").build();
        shop.setId(2L);
        Order order = Order.builder()
                .orderCode("ORD123")
                .user(group.getUser())
                .shop(shop)
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
        order.getItems()
                .add(OrderItem.builder()
                        .order(order)
                        .productVariationId(41L)
                        .productName("Dress")
                        .sku("DRESS-001")
                        .basePrice(new BigDecimal("50.00"))
                        .additionalPrice(BigDecimal.ZERO)
                        .finalPrice(new BigDecimal("50.00"))
                        .quantity(2)
                        .build());
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
}
