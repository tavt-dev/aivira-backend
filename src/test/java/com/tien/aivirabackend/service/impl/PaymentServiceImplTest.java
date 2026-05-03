package com.tien.aivirabackend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tien.aivirabackend.config.properties.PaymentProperties;
import com.tien.aivirabackend.constant.OrderStatus;
import com.tien.aivirabackend.constant.PaymentMethod;
import com.tien.aivirabackend.constant.PaymentProvider;
import com.tien.aivirabackend.constant.PaymentStatus;
import com.tien.aivirabackend.domain.dto.RequestMetadata;
import com.tien.aivirabackend.domain.dto.response.PaymentReconciliationResponse;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.entity.catalog.ProductVariation;
import com.tien.aivirabackend.domain.entity.transaction.Order;
import com.tien.aivirabackend.domain.entity.transaction.OrderItem;
import com.tien.aivirabackend.domain.entity.transaction.payment.Payment;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentAttempt;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentCallback;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentGroup;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.mapper.CommerceMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.repository.*;
import com.tien.aivirabackend.service.CurrentUserService;
import com.tien.aivirabackend.service.commerce.InventoryService;
import com.tien.aivirabackend.service.payment.*;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {
    @Mock
    PaymentGroupRepository paymentGroupRepository;

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    PaymentAttemptRepository paymentAttemptRepository;

    @Mock
    PaymentCallbackRepository paymentCallbackRepository;

    @Mock
    OrderRepository orderRepository;

    @Mock
    CartItemRepository cartItemRepository;

    @Mock
    ProductVariationRepository variationRepository;

    @Mock
    CurrentUserService currentUserService;

    @Mock
    PaymentProviderClient providerClient;

    PaymentServiceImpl paymentService;
    SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        PaymentProperties paymentProperties = new PaymentProperties();
        paymentProperties.setPendingTtlMinutes(15);
        meterRegistry = new SimpleMeterRegistry();
        PaymentProviderSupportService paymentProviderSupportService =
                new PaymentProviderSupportService(paymentAttemptRepository, List.of(providerClient), meterRegistry);
        paymentService = new PaymentServiceImpl(
                paymentGroupRepository,
                paymentRepository,
                paymentAttemptRepository,
                paymentCallbackRepository,
                orderRepository,
                cartItemRepository,
                paymentProperties,
                currentUserService,
                new CommerceMapper(),
                paymentProviderSupportService,
                new ObjectMapper(),
                meterRegistry,
                new InventoryService(variationRepository),
                new PaymentAttemptResolver(paymentAttemptRepository, paymentGroupRepository));
        lenient().when(providerClient.method()).thenReturn(PaymentMethod.VNPAY);
    }

    @Test
    void handleCallback_whenPendingSuccess_shouldMarkPaidAndStoreJsonCallback() {
        PaymentGroup group = paymentGroup(PaymentStatus.PENDING);
        PaymentAttempt attempt = attempt(group, PaymentStatus.PENDING);
        Order order = order(group, OrderStatus.PENDING_PAYMENT);
        PaymentProviderCallbackResult callbackResult = callbackResult(PaymentStatus.SUCCESS, "TXN-1", "EVT-1");

        when(providerClient.verifyCallback(anyMap())).thenReturn(true);
        when(providerClient.parseCallback(anyMap())).thenReturn(callbackResult);
        when(paymentAttemptRepository.findByProviderAndProviderTxnRefForUpdate(PaymentProvider.VNPAY, "PAY123-A1"))
                .thenReturn(Optional.of(attempt));
        when(paymentCallbackRepository.findByProviderAndEventKey(PaymentProvider.VNPAY, "EVT-1"))
                .thenReturn(Optional.empty());
        when(paymentGroupRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(orderRepository.findByPaymentsPaymentGroupId(group.getId())).thenReturn(List.of(order));

        paymentService.handleVnpayCallback(Map.of("vnp_TxnRef", "PAY123-A1"), false);

        assertThat(group.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(group.getProviderTransactionId()).isEqualTo("TXN-1");
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getPayments().getFirst().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(attempt.getStatus()).isEqualTo(PaymentStatus.SUCCESS);

        ArgumentCaptor<PaymentCallback> callbackCaptor = ArgumentCaptor.forClass(PaymentCallback.class);
        verify(paymentCallbackRepository).save(callbackCaptor.capture());
        assertThat(callbackCaptor.getValue().getStatus()).isEqualTo("SUCCESS");
        assertThat(callbackCaptor.getValue().getRawPayload()).contains("\"providerTxnRef\":\"PAY123-A1\"");
    }

    @Test
    void handleCallback_whenDuplicateEvent_shouldNotChangePendingState() {
        PaymentGroup group = paymentGroup(PaymentStatus.PENDING);
        PaymentAttempt attempt = attempt(group, PaymentStatus.PENDING);
        PaymentProviderCallbackResult callbackResult = callbackResult(PaymentStatus.SUCCESS, "TXN-1", "EVT-1");

        when(providerClient.verifyCallback(anyMap())).thenReturn(true);
        when(providerClient.parseCallback(anyMap())).thenReturn(callbackResult);
        when(paymentAttemptRepository.findByProviderAndProviderTxnRefForUpdate(PaymentProvider.VNPAY, "PAY123-A1"))
                .thenReturn(Optional.of(attempt));
        when(paymentCallbackRepository.findByProviderAndEventKey(PaymentProvider.VNPAY, "EVT-1"))
                .thenReturn(
                        Optional.of(PaymentCallback.builder().eventKey("EVT-1").build()));
        when(paymentGroupRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(orderRepository.findByPaymentsPaymentGroupId(group.getId())).thenReturn(List.of());

        paymentService.handleVnpayCallback(Map.of("vnp_TxnRef", "PAY123-A1"), false);

        assertThat(group.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentCallbackRepository, never()).save(any());
        verify(orderRepository, never()).saveAll(any());
    }

    @Test
    void handleCallback_whenLateSuccessAfterExpired_shouldOnlyStoreConflictCallback() {
        PaymentGroup group = paymentGroup(PaymentStatus.EXPIRED);
        PaymentAttempt attempt = attempt(group, PaymentStatus.EXPIRED);
        Order order = order(group, OrderStatus.EXPIRED);
        PaymentProviderCallbackResult callbackResult = callbackResult(PaymentStatus.SUCCESS, "TXN-1", "EVT-LATE");

        when(providerClient.verifyCallback(anyMap())).thenReturn(true);
        when(providerClient.parseCallback(anyMap())).thenReturn(callbackResult);
        when(paymentAttemptRepository.findByProviderAndProviderTxnRefForUpdate(PaymentProvider.VNPAY, "PAY123-A1"))
                .thenReturn(Optional.of(attempt));
        when(paymentCallbackRepository.findByProviderAndEventKey(PaymentProvider.VNPAY, "EVT-LATE"))
                .thenReturn(Optional.empty());
        when(paymentGroupRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(orderRepository.findByPaymentsPaymentGroupId(group.getId())).thenReturn(List.of(order));

        paymentService.handleVnpayCallback(Map.of("vnp_TxnRef", "PAY123-A1"), false);

        assertThat(group.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.EXPIRED);
        ArgumentCaptor<PaymentCallback> callbackCaptor = ArgumentCaptor.forClass(PaymentCallback.class);
        verify(paymentCallbackRepository).save(callbackCaptor.capture());
        assertThat(callbackCaptor.getValue().getStatus()).isEqualTo("CONFLICT_SUCCESS_AFTER_EXPIRED");
    }

    @Test
    void handleCallback_whenPendingFailed_shouldMarkFailedAndRestoreStock() {
        PaymentGroup group = paymentGroup(PaymentStatus.PENDING);
        PaymentAttempt attempt = attempt(group, PaymentStatus.PENDING);
        Order order = orderWithItem(group, OrderStatus.PENDING_PAYMENT);
        ProductVariation variation = variation(8);
        PaymentProviderCallbackResult callbackResult = callbackResult(PaymentStatus.FAILED, "TXN-FAILED", "EVT-FAILED");

        when(providerClient.verifyCallback(anyMap())).thenReturn(true);
        when(providerClient.parseCallback(anyMap())).thenReturn(callbackResult);
        when(paymentAttemptRepository.findByProviderAndProviderTxnRefForUpdate(PaymentProvider.VNPAY, "PAY123-A1"))
                .thenReturn(Optional.of(attempt));
        when(paymentCallbackRepository.findByProviderAndEventKey(PaymentProvider.VNPAY, "EVT-FAILED"))
                .thenReturn(Optional.empty());
        when(paymentGroupRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(orderRepository.findByPaymentsPaymentGroupId(group.getId())).thenReturn(List.of(order));
        when(variationRepository.findAllByIdInForUpdate(anyCollection())).thenReturn(List.of(variation));

        paymentService.handleVnpayCallback(Map.of("vnp_TxnRef", "PAY123-A1"), false);

        assertThat(group.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(order.getPayments().getFirst().getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(variation.getStockQuantity()).isEqualTo(10);
    }

    @Test
    void handleCallback_whenAmountMismatch_shouldNotChangeState() {
        PaymentGroup group = paymentGroup(PaymentStatus.PENDING);
        PaymentAttempt attempt = attempt(group, PaymentStatus.PENDING);
        PaymentProviderCallbackResult callbackResult = new PaymentProviderCallbackResult(
                "PAY123-A1",
                "PAY123-A1-REQ",
                "TXN-1",
                new BigDecimal("999.00"),
                PaymentStatus.SUCCESS,
                "EVT-MISMATCH",
                Map.of("providerTxnRef", "PAY123-A1"));

        when(providerClient.verifyCallback(anyMap())).thenReturn(true);
        when(providerClient.parseCallback(anyMap())).thenReturn(callbackResult);
        when(paymentAttemptRepository.findByProviderAndProviderTxnRefForUpdate(PaymentProvider.VNPAY, "PAY123-A1"))
                .thenReturn(Optional.of(attempt));
        when(paymentCallbackRepository.findByProviderAndEventKey(PaymentProvider.VNPAY, "EVT-MISMATCH"))
                .thenReturn(Optional.empty());
        when(paymentGroupRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> paymentService.handleVnpayCallback(Map.of("vnp_TxnRef", "PAY123-A1"), false))
                .isInstanceOf(AppException.class);

        assertThat(group.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(attempt.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verify(paymentCallbackRepository, never()).save(any());
        verify(orderRepository, never()).saveAll(any());
    }

    @Test
    void retry_whenFailedOnlinePayment_shouldCreateNewAttemptWithoutOverwritingOldAttempt() {
        User user = user();
        PaymentGroup group = paymentGroup(PaymentStatus.FAILED);
        Order order = orderWithItem(group, OrderStatus.PAYMENT_FAILED);
        ProductVariation variation = variation(10);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(paymentGroupRepository.findByPaymentCodeAndUserId("PAY123", user.getId()))
                .thenReturn(Optional.of(group));
        when(orderRepository.findByPaymentsPaymentGroupId(group.getId())).thenReturn(List.of(order));
        when(variationRepository.findAllByIdInForUpdate(anyCollection())).thenReturn(List.of(variation));
        when(paymentAttemptRepository.countByPaymentGroupId(group.getId())).thenReturn(1);
        when(paymentAttemptRepository.save(any(PaymentAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(providerClient.createPayment(any(PaymentProviderRequest.class)))
                .thenReturn(new PaymentProviderResult(
                        "PAY123-A2-NEW", "PAY123-A2-NEW-REQ", null, "https://pay", null, null, "{}", "{}"));

        paymentService.retry("PAY123", new RequestMetadata("JUnit", "127.0.0.1"));

        assertThat(group.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(variation.getStockQuantity()).isEqualTo(8);
        ArgumentCaptor<PaymentAttempt> attemptCaptor = ArgumentCaptor.forClass(PaymentAttempt.class);
        verify(paymentAttemptRepository, atLeastOnce()).save(attemptCaptor.capture());
        assertThat(attemptCaptor.getAllValues()).anySatisfy(savedAttempt -> {
            assertThat(savedAttempt.getAttemptNo()).isEqualTo(2);
            assertThat(savedAttempt.getProviderTxnRef()).isEqualTo("PAY123-A2-NEW");
        });
    }

    @Test
    void reconcile_whenProviderSuccess_shouldUseSameStateTransition() {
        PaymentGroup group = paymentGroup(PaymentStatus.PENDING);
        PaymentAttempt attempt = attempt(group, PaymentStatus.PENDING);
        Order order = order(group, OrderStatus.PENDING_PAYMENT);

        when(paymentGroupRepository.findByPaymentCode("PAY123")).thenReturn(Optional.of(group));
        when(paymentAttemptRepository.findTopByPaymentGroupIdOrderByAttemptNoDesc(group.getId()))
                .thenReturn(Optional.of(attempt));
        when(providerClient.queryPayment(attempt))
                .thenReturn(new PaymentProviderQueryResult(
                        "PAY123-A1",
                        "PAY123-A1-REQ",
                        "TXN-QUERY",
                        group.getAmount(),
                        PaymentStatus.SUCCESS,
                        "Query success",
                        "{}"));
        when(paymentAttemptRepository.findByIdForUpdate(attempt.getId())).thenReturn(Optional.of(attempt));
        when(paymentCallbackRepository.findByProviderAndEventKey(eq(PaymentProvider.VNPAY), startsWith("RECONCILE:")))
                .thenReturn(Optional.empty());
        when(paymentGroupRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(orderRepository.findByPaymentsPaymentGroupId(group.getId())).thenReturn(List.of(order));

        PaymentReconciliationResponse response = paymentService.reconcile("PAY123");

        assertThat(response.isChanged()).isTrue();
        assertThat(response.getLocalStatusBefore()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.getLocalStatusAfter()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void reconcile_whenCodPayment_shouldReject() {
        PaymentGroup group = paymentGroup(PaymentStatus.PENDING);
        group.setMethod(PaymentMethod.COD);

        when(paymentGroupRepository.findByPaymentCode("PAY123")).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> paymentService.reconcile("PAY123")).isInstanceOf(AppException.class);

        verify(paymentAttemptRepository, never()).findTopByPaymentGroupIdOrderByAttemptNoDesc(anyLong());
    }

    private PaymentGroup paymentGroup(PaymentStatus status) {
        PaymentGroup group = PaymentGroup.builder()
                .paymentCode("PAY123")
                .user(user())
                .method(PaymentMethod.VNPAY)
                .status(status)
                .amount(new BigDecimal("1234.00"))
                .expiresAt(Instant.now().plusSeconds(900))
                .build();
        group.setId(1L);
        return group;
    }

    private PaymentAttempt attempt(PaymentGroup group, PaymentStatus status) {
        PaymentAttempt attempt = PaymentAttempt.builder()
                .paymentGroup(group)
                .provider(PaymentProvider.VNPAY)
                .method(PaymentMethod.VNPAY)
                .attemptNo(1)
                .providerTxnRef("PAY123-A1")
                .requestId("PAY123-A1-REQ")
                .status(status)
                .amount(group.getAmount())
                .expiresAt(group.getExpiresAt())
                .build();
        attempt.setId(11L);
        return attempt;
    }

    private Order order(PaymentGroup group, OrderStatus orderStatus) {
        Order order = Order.builder()
                .orderCode("ORD123")
                .user(group.getUser())
                .orderStatus(orderStatus)
                .totalAmount(group.getAmount())
                .build();
        order.setId(21L);
        Payment payment = Payment.builder()
                .order(order)
                .paymentGroup(group)
                .method(PaymentMethod.VNPAY)
                .status(group.getStatus())
                .amount(group.getAmount())
                .build();
        payment.setId(31L);
        order.getPayments().add(payment);
        group.getPayments().add(payment);
        return order;
    }

    private Order orderWithItem(PaymentGroup group, OrderStatus orderStatus) {
        Order order = order(group, orderStatus);
        order.getItems()
                .add(OrderItem.builder()
                        .order(order)
                        .productVariationId(41L)
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

    private PaymentProviderCallbackResult callbackResult(PaymentStatus status, String transactionId, String eventKey) {
        return new PaymentProviderCallbackResult(
                "PAY123-A1",
                "PAY123-A1-REQ",
                transactionId,
                new BigDecimal("1234.00"),
                status,
                eventKey,
                Map.of("providerTxnRef", "PAY123-A1", "transactionId", transactionId));
    }
}
