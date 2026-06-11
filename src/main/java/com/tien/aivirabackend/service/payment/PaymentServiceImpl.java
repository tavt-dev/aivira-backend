package com.tien.aivirabackend.service.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tien.aivirabackend.config.properties.PaymentProperties;
import com.tien.aivirabackend.constant.OrderStatus;
import com.tien.aivirabackend.constant.PaymentMethod;
import com.tien.aivirabackend.constant.PaymentProvider;
import com.tien.aivirabackend.constant.PaymentStatus;
import com.tien.aivirabackend.domain.dto.RequestMetadata;
import com.tien.aivirabackend.domain.dto.response.PaymentGroupResponse;
import com.tien.aivirabackend.domain.dto.response.PaymentReconciliationResponse;
import com.tien.aivirabackend.domain.dto.response.PaymentResponse;
import com.tien.aivirabackend.domain.dto.response.VnpayIpnResponse;
import com.tien.aivirabackend.domain.entity.transaction.Order;
import com.tien.aivirabackend.domain.entity.transaction.payment.Payment;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentAttempt;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentCallback;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentGroup;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.mapper.CommerceMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.PaymentErrorCode;
import com.tien.aivirabackend.repository.CartItemRepository;
import com.tien.aivirabackend.repository.OrderRepository;
import com.tien.aivirabackend.repository.PaymentAttemptRepository;
import com.tien.aivirabackend.repository.PaymentCallbackRepository;
import com.tien.aivirabackend.repository.PaymentGroupRepository;
import com.tien.aivirabackend.repository.PaymentRepository;
import com.tien.aivirabackend.service.auth.CurrentUserService;
import com.tien.aivirabackend.service.commerce.InventoryService;
import com.tien.aivirabackend.service.discount.DiscountService;
import com.tien.aivirabackend.service.payment.provider.PaymentProviderCallbackResult;
import com.tien.aivirabackend.service.payment.provider.PaymentProviderClient;
import com.tien.aivirabackend.service.payment.provider.PaymentProviderQueryResult;
import com.tien.aivirabackend.service.payment.provider.PaymentProviderResult;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j(topic = "PAYMENT-SERVICE")
public class PaymentServiceImpl implements PaymentService {
    PaymentGroupRepository paymentGroupRepository;
    PaymentRepository paymentRepository;
    PaymentAttemptRepository paymentAttemptRepository;
    PaymentCallbackRepository paymentCallbackRepository;
    OrderRepository orderRepository;
    CartItemRepository cartItemRepository;
    PaymentProperties paymentProperties;
    CurrentUserService currentUserService;
    CommerceMapper commerceMapper;
    PaymentProviderSupportService paymentProviderSupportService;
    ObjectMapper objectMapper;
    MeterRegistry meterRegistry;
    InventoryService inventoryService;
    PaymentAttemptResolver paymentAttemptResolver;
    DiscountService discountService;

    @Override
    @Transactional(readOnly = true)
    public PaymentGroupResponse getMyPaymentGroup(String paymentGroupCode) {
        User user = currentUserService.getCurrentUser();
        PaymentGroup group = paymentGroupRepository
                .findByPaymentCodeAndUserId(paymentGroupCode, user.getId())
                .orElseThrow(() -> new AppException(PaymentErrorCode.PAYMENT_GROUP_NOT_FOUND));
        return toPaymentGroupResponse(group);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentGroupResponse getAdminPaymentGroup(String paymentGroupCode) {
        PaymentGroup group = paymentGroupRepository
                .findByPaymentCode(paymentGroupCode)
                .orElseThrow(() -> new AppException(PaymentErrorCode.PAYMENT_GROUP_NOT_FOUND));
        return toPaymentGroupResponse(group);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getMyPayment(Long paymentId) {
        User user = currentUserService.getCurrentUser();
        Payment payment = paymentRepository
                .findByIdAndOrderUserId(paymentId, user.getId())
                .orElseThrow(() -> new AppException(PaymentErrorCode.PAYMENT_NOT_FOUND));
        return commerceMapper.toPaymentResponse(payment);
    }

    @Override
    @Transactional
    public PaymentGroupResponse retry(String paymentGroupCode, RequestMetadata requestMetadata) {
        User user = currentUserService.getCurrentUser();
        PaymentGroup group = paymentGroupRepository
                .findByPaymentCodeAndUserId(paymentGroupCode, user.getId())
                .orElseThrow(() -> new AppException(PaymentErrorCode.PAYMENT_GROUP_NOT_FOUND));
        if (group.getMethod() == PaymentMethod.COD
                || (group.getStatus() != PaymentStatus.FAILED
                        && group.getStatus() != PaymentStatus.CANCELLED
                        && group.getStatus() != PaymentStatus.EXPIRED)) {
            throw new AppException(PaymentErrorCode.PAYMENT_INVALID_STATUS);
        }
        List<Order> orders = orderRepository.findByPaymentsPaymentGroupId(group.getId());
        discountService.reserveReleasedCouponUsagesForRetry(orders);
        inventoryService.deductStockForOrders(orders);
        PaymentAttempt attempt = paymentProviderSupportService.createAttempt(group);
        group.setStatus(PaymentStatus.PENDING);
        group.setExpiresAt(Instant.now().plusSeconds(paymentProperties.getPendingTtlMinutes() * 60));
        group.setPaidAt(null);
        attempt.setExpiresAt(group.getExpiresAt());
        orders.forEach(order -> {
            order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
            order.getPayments().forEach(payment -> {
                payment.setStatus(PaymentStatus.PENDING);
                payment.setPaidAt(null);
                payment.setTransactionId(null);
            });
        });
        PaymentProviderResult result = paymentProviderSupportService.createPaymentWithMetrics(
                provider(group.getMethod()), group, attempt, requestMetadata);
        paymentProviderSupportService.applyProviderResult(group, attempt, result);
        paymentGroupRepository.save(group);
        paymentAttemptRepository.save(attempt);
        orderRepository.saveAll(orders);
        return toPaymentGroupResponse(group);
    }

    @Override
    @Transactional
    public PaymentGroupResponse handleVnpayCallback(Map<String, String> params, boolean returnRequest) {
        PaymentProviderClient provider = provider(PaymentMethod.VNPAY);
        if (!provider.verifyCallback(params)) {
            throw new AppException(PaymentErrorCode.PAYMENT_INVALID_SIGNATURE);
        }
        PaymentProviderCallbackResult result = provider.parseCallback(params);
        PaymentAttempt attempt = paymentAttemptResolver.resolveForUpdate(PaymentProvider.VNPAY, result);
        return applyProviderState(
                attempt,
                result.amount(),
                result.status(),
                result.transactionId(),
                PaymentProvider.VNPAY,
                result.eventKey(),
                result.rawPayload());
    }

    @Override
    @Transactional
    public VnpayIpnResponse handleVnpayIpn(Map<String, String> params) {
        try {
            PaymentProviderClient provider = provider(PaymentMethod.VNPAY);
            if (!provider.verifyCallback(params)) {
                return new VnpayIpnResponse("97", "Invalid Checksum");
            }
            PaymentProviderCallbackResult result = provider.parseCallback(params);
            if (paymentCallbackRepository
                    .findByProviderAndEventKey(PaymentProvider.VNPAY, result.eventKey())
                    .isPresent()) {
                return new VnpayIpnResponse("02", "Order already confirmed");
            }
            handleVnpayCallback(params, false);
            return new VnpayIpnResponse("00", "Confirm Success");
        } catch (AppException ex) {
            if (ex.getErrorCode() == PaymentErrorCode.PAYMENT_INVALID_SIGNATURE) {
                return new VnpayIpnResponse("97", "Invalid Checksum");
            }
            if (ex.getErrorCode() == PaymentErrorCode.PAYMENT_GROUP_NOT_FOUND) {
                return new VnpayIpnResponse("01", "Order not Found");
            }
            if (ex.getErrorCode() == PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH) {
                return new VnpayIpnResponse("04", "Invalid Amount");
            }
            log.warn("VNPay IPN failed: code={} message={}", ex.getErrorCode().getCode(), ex.getMessage());
            return new VnpayIpnResponse("99", "Unknown error");
        } catch (RuntimeException ex) {
            log.error("VNPay IPN failed unexpectedly", ex);
            return new VnpayIpnResponse("99", "Unknown error");
        }
    }

    @Override
    @Transactional
    public PaymentGroupResponse handleMomoIpn(Map<String, Object> payload) {
        PaymentProviderClient provider = provider(PaymentMethod.MOMO);
        if (!provider.verifyCallback(payload)) {
            throw new AppException(PaymentErrorCode.PAYMENT_INVALID_SIGNATURE);
        }
        PaymentProviderCallbackResult result = provider.parseCallback(payload);
        PaymentAttempt attempt = paymentAttemptResolver.resolveForUpdate(PaymentProvider.MOMO, result);
        return applyProviderState(
                attempt,
                result.amount(),
                result.status(),
                result.transactionId(),
                PaymentProvider.MOMO,
                result.eventKey(),
                result.rawPayload());
    }

    @Override
    @Transactional
    public PaymentReconciliationResponse reconcile(String paymentGroupCode) {
        PaymentGroup group = paymentGroupRepository
                .findByPaymentCode(paymentGroupCode)
                .orElseThrow(() -> new AppException(PaymentErrorCode.PAYMENT_GROUP_NOT_FOUND));
        if (group.getMethod() == PaymentMethod.COD) {
            throw new AppException(PaymentErrorCode.PAYMENT_INVALID_STATUS);
        }
        PaymentAttempt attempt = paymentAttemptRepository
                .findTopByPaymentGroupIdOrderByAttemptNoDesc(group.getId())
                .orElseThrow(() -> new AppException(PaymentErrorCode.PAYMENT_GROUP_NOT_FOUND));
        PaymentProviderQueryResult queryResult =
                paymentProviderSupportService.queryPaymentWithMetrics(provider(group.getMethod()), attempt);
        increment(
                "payment_reconciliation_total",
                "method",
                group.getMethod().name(),
                "result",
                queryResult.status().name());
        PaymentStatus before = group.getStatus();
        if (queryResult.amount().compareTo(group.getAmount()) != 0) {
            throw new AppException(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
        PaymentAttempt lockedAttempt = paymentAttemptRepository
                .findByIdForUpdate(attempt.getId())
                .orElseThrow(() -> new AppException(PaymentErrorCode.PAYMENT_GROUP_NOT_FOUND));
        applyProviderState(
                lockedAttempt,
                queryResult.amount(),
                queryResult.status(),
                queryResult.transactionId(),
                PaymentProvider.valueOf(group.getMethod().name()),
                "RECONCILE:" + lockedAttempt.getProvider() + ":" + lockedAttempt.getProviderTxnRef() + ":"
                        + Instant.now().toEpochMilli(),
                Map.of("rawResponse", queryResult.rawResponse() == null ? "" : queryResult.rawResponse()));
        PaymentStatus after = lockedAttempt.getPaymentGroup().getStatus();
        return PaymentReconciliationResponse.builder()
                .paymentGroupCode(paymentGroupCode)
                .method(group.getMethod())
                .providerTxnRef(lockedAttempt.getProviderTxnRef())
                .localStatusBefore(before)
                .localStatusAfter(after)
                .providerStatus(queryResult.status())
                .changed(before != after)
                .message(queryResult.message())
                .checkedAt(Instant.now())
                .build();
    }

    @Override
    @Transactional
    public void expirePendingPayments() {
        List<PaymentGroup> expiredGroups = paymentGroupRepository.findByStatusAndMethodNotAndExpiresAtBefore(
                PaymentStatus.PENDING, PaymentMethod.COD, Instant.now());
        for (PaymentGroup group : expiredGroups) {
            paymentAttemptRepository
                    .findTopByPaymentGroupIdOrderByAttemptNoDesc(group.getId())
                    .ifPresent(attempt -> {
                        attempt.setStatus(PaymentStatus.EXPIRED);
                        attempt.setCompletedAt(Instant.now());
                        paymentAttemptRepository.save(attempt);
                    });
            applyTerminalFailure(group, PaymentStatus.EXPIRED, OrderStatus.EXPIRED);
        }
    }

    private PaymentGroupResponse applyProviderState(
            PaymentAttempt attempt,
            BigDecimal providerAmount,
            PaymentStatus targetStatus,
            String transactionId,
            PaymentProvider provider,
            String eventKey,
            Map<String, ?> rawPayload) {
        Optional<PaymentCallback> existing = paymentCallbackRepository.findByProviderAndEventKey(provider, eventKey);
        PaymentGroup group = paymentGroupRepository
                .findByIdForUpdate(attempt.getPaymentGroup().getId())
                .orElseThrow(() -> new AppException(PaymentErrorCode.PAYMENT_GROUP_NOT_FOUND));
        attempt.setPaymentGroup(group);
        if (existing.isPresent()) {
            return toPaymentGroupResponse(group);
        }
        if (providerAmount.compareTo(group.getAmount()) != 0) {
            throw new AppException(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
        String callbackStatus = targetStatus.name();
        if (group.getStatus() == PaymentStatus.PENDING) {
            if (targetStatus == PaymentStatus.SUCCESS) {
                applySuccess(group, attempt, transactionId);
            } else {
                attempt.setStatus(targetStatus);
                attempt.setCompletedAt(Instant.now());
                applyTerminalFailure(group, targetStatus, OrderStatus.PAYMENT_FAILED);
            }
        } else if (group.getStatus() == PaymentStatus.SUCCESS && targetStatus == PaymentStatus.SUCCESS) {
            callbackStatus = "DUPLICATE_SUCCESS";
        } else if (targetStatus == PaymentStatus.SUCCESS) {
            callbackStatus = "CONFLICT_SUCCESS_AFTER_" + group.getStatus().name();
            log.warn(
                    "payment_late_success_conflict paymentGroupCode={} attemptId={} provider={} currentStatus={}",
                    group.getPaymentCode(),
                    attempt.getId(),
                    provider,
                    group.getStatus());
        } else {
            callbackStatus = "DUPLICATE_" + group.getStatus().name();
        }
        paymentAttemptRepository.save(attempt);
        paymentCallbackRepository.save(PaymentCallback.builder()
                .provider(provider)
                .eventKey(eventKey)
                .paymentCode(group.getPaymentCode())
                .status(callbackStatus)
                .rawPayload(toJson(rawPayload))
                .build());
        increment("payment_callback_total", "provider", provider.name(), "result", callbackStatus);
        return toPaymentGroupResponse(group);
    }

    private void applySuccess(PaymentGroup group, PaymentAttempt attempt, String transactionId) {
        Instant now = Instant.now();
        group.setStatus(PaymentStatus.SUCCESS);
        group.setProviderTransactionId(transactionId);
        group.setPaidAt(now);
        attempt.setStatus(PaymentStatus.SUCCESS);
        attempt.setProviderTransactionId(transactionId);
        attempt.setCompletedAt(now);
        List<Order> orders = orderRepository.findByPaymentsPaymentGroupId(group.getId());
        orders.forEach(order -> {
            order.setOrderStatus(OrderStatus.PAID);
            order.getPayments().forEach(payment -> {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setTransactionId(transactionId);
                payment.setPaidAt(now);
            });
        });
        discountService.finalizeReservedCouponUsagesForOrders(orders);
        clearCartItemsForOrders(group.getUser().getId(), orders);
        orderRepository.saveAll(orders);
        paymentGroupRepository.save(group);
    }

    private void applyTerminalFailure(PaymentGroup group, PaymentStatus paymentStatus, OrderStatus orderStatus) {
        if (group.getStatus() != PaymentStatus.PENDING) {
            return;
        }
        group.setStatus(paymentStatus);
        List<Order> orders = orderRepository.findByPaymentsPaymentGroupId(group.getId());
        inventoryService.restoreStockForOrders(orders);
        discountService.releaseReservedCouponUsagesForOrders(orders);
        orders.forEach(order -> {
            order.setOrderStatus(orderStatus);
            order.getPayments().forEach(payment -> payment.setStatus(paymentStatus));
        });
        orderRepository.saveAll(orders);
        paymentGroupRepository.save(group);
    }

    private void clearCartItemsForOrders(String userId, List<Order> orders) {
        Set<Long> variationIds =
                inventoryService.orderVariationQuantities(orders).keySet();
        if (!variationIds.isEmpty()) {
            cartItemRepository.deleteActiveCartItemsByUserIdAndVariationIds(userId, variationIds);
        }
    }

    private PaymentProviderClient provider(PaymentMethod method) {
        return paymentProviderSupportService.provider(
                method, () -> new AppException(PaymentErrorCode.PAYMENT_PROVIDER_ERROR));
    }

    private PaymentGroupResponse toPaymentGroupResponse(PaymentGroup group) {
        List<Order> orders = orderRepository.findByPaymentsPaymentGroupId(group.getId());
        return commerceMapper.toPaymentGroupResponse(group, orders);
    }

    private String toJson(Map<String, ?> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException ex) {
            return payload.toString();
        }
    }

    private void increment(String metricName, String... tags) {
        meterRegistry.counter(metricName, tags).increment();
    }
}
