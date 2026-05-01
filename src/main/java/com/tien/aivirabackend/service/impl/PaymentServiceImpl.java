package com.tien.aivirabackend.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tien.aivirabackend.config.properties.PaymentProperties;
import com.tien.aivirabackend.constant.*;
import com.tien.aivirabackend.domain.dto.response.PaymentGroupResponse;
import com.tien.aivirabackend.domain.dto.response.PaymentResponse;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.entity.catalog.ProductVariation;
import com.tien.aivirabackend.domain.entity.transaction.Order;
import com.tien.aivirabackend.domain.entity.transaction.OrderItem;
import com.tien.aivirabackend.domain.entity.transaction.payment.Payment;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentCallback;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentGroup;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.mapper.CommerceMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.CartErrorCode;
import com.tien.aivirabackend.exception.errorCode.PaymentErrorCode;
import com.tien.aivirabackend.repository.*;
import com.tien.aivirabackend.service.CurrentUserService;
import com.tien.aivirabackend.service.PaymentService;
import com.tien.aivirabackend.service.payment.PaymentProviderClient;
import com.tien.aivirabackend.service.payment.PaymentProviderResult;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentServiceImpl implements PaymentService {
    PaymentGroupRepository paymentGroupRepository;
    PaymentRepository paymentRepository;
    PaymentCallbackRepository paymentCallbackRepository;
    OrderRepository orderRepository;
    CartItemRepository cartItemRepository;
    ProductVariationRepository variationRepository;
    PaymentProperties paymentProperties;
    CurrentUserService currentUserService;
    CommerceMapper commerceMapper;
    List<PaymentProviderClient> paymentProviderClients;

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
    public PaymentResponse getMyPayment(Long paymentId) {
        User user = currentUserService.getCurrentUser();
        Payment payment = paymentRepository
                .findByIdAndOrderUserId(paymentId, user.getId())
                .orElseThrow(() -> new AppException(PaymentErrorCode.PAYMENT_NOT_FOUND));
        return commerceMapper.toPaymentResponse(payment);
    }

    @Override
    @Transactional
    public PaymentGroupResponse retry(String paymentGroupCode) {
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
        deductStockForOrders(orders);
        group.setStatus(PaymentStatus.PENDING);
        group.setExpiresAt(Instant.now().plusSeconds(paymentProperties.getPendingTtlMinutes() * 60));
        group.setPaidAt(null);
        orders.forEach(order -> {
            order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
            order.getPayments().forEach(payment -> {
                payment.setStatus(PaymentStatus.PENDING);
                payment.setPaidAt(null);
                payment.setTransactionId(null);
            });
        });
        PaymentProviderResult result = provider(group.getMethod()).createPayment(group);
        applyProviderResult(group, result);
        paymentGroupRepository.save(group);
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
        String txnRef = params.get("vnp_TxnRef");
        String eventKey = "VNPAY:" + txnRef + ":" + params.getOrDefault("vnp_TransactionNo", params.toString());
        Optional<PaymentCallback> existing =
                paymentCallbackRepository.findByProviderAndEventKey(PaymentProvider.VNPAY, eventKey);
        PaymentGroup group = paymentGroupRepository
                .findByProviderTxnRef(txnRef)
                .orElseThrow(() -> new AppException(PaymentErrorCode.PAYMENT_GROUP_NOT_FOUND));
        if (existing.isPresent()) {
            return toPaymentGroupResponse(group);
        }
        BigDecimal callbackAmount =
                new BigDecimal(params.getOrDefault("vnp_Amount", "0")).divide(BigDecimal.valueOf(100));
        if (callbackAmount.compareTo(group.getAmount()) != 0) {
            throw new AppException(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
        boolean success =
                "00".equals(params.get("vnp_ResponseCode")) && "00".equals(params.get("vnp_TransactionStatus"));
        String transactionId = params.get("vnp_TransactionNo");
        PaymentGroupResponse response = applyCallbackResult(
                group,
                success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED,
                transactionId,
                PaymentProvider.VNPAY,
                eventKey,
                params);
        return response;
    }

    @Override
    @Transactional
    public PaymentGroupResponse handleMomoIpn(Map<String, Object> payload) {
        PaymentProviderClient provider = provider(PaymentMethod.MOMO);
        if (!provider.verifyCallback(payload)) {
            throw new AppException(PaymentErrorCode.PAYMENT_INVALID_SIGNATURE);
        }
        String orderId = stringValue(payload.get("orderId"));
        String requestId = stringValue(payload.get("requestId"));
        String transId = stringValue(payload.get("transId"));
        String eventKey = "MOMO:" + orderId + ":" + (transId.isBlank() ? requestId : transId);
        Optional<PaymentCallback> existing =
                paymentCallbackRepository.findByProviderAndEventKey(PaymentProvider.MOMO, eventKey);
        PaymentGroup group = paymentGroupRepository
                .findByPaymentCode(orderId)
                .orElseThrow(() -> new AppException(PaymentErrorCode.PAYMENT_GROUP_NOT_FOUND));
        if (existing.isPresent()) {
            return toPaymentGroupResponse(group);
        }
        BigDecimal callbackAmount = new BigDecimal(stringValue(payload.get("amount")));
        if (callbackAmount.compareTo(group.getAmount()) != 0) {
            throw new AppException(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
        boolean success = "0".equals(stringValue(payload.get("resultCode")));
        return applyCallbackResult(
                group,
                success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED,
                transId,
                PaymentProvider.MOMO,
                eventKey,
                payload);
    }

    @Override
    @Transactional
    public void expirePendingPayments() {
        List<PaymentGroup> expiredGroups = paymentGroupRepository.findByStatusAndMethodNotAndExpiresAtBefore(
                PaymentStatus.PENDING, PaymentMethod.COD, Instant.now());
        for (PaymentGroup group : expiredGroups) {
            applyTerminalFailure(group, PaymentStatus.EXPIRED, OrderStatus.EXPIRED);
        }
    }

    private PaymentGroupResponse applyCallbackResult(
            PaymentGroup group,
            PaymentStatus targetStatus,
            String transactionId,
            PaymentProvider provider,
            String eventKey,
            Map<String, ?> rawPayload) {
        if (group.getStatus() == PaymentStatus.PENDING) {
            if (targetStatus == PaymentStatus.SUCCESS) {
                Instant now = Instant.now();
                group.setStatus(PaymentStatus.SUCCESS);
                group.setProviderTransactionId(transactionId);
                group.setPaidAt(now);
                List<Order> orders = orderRepository.findByPaymentsPaymentGroupId(group.getId());
                orders.forEach(order -> {
                    order.setOrderStatus(OrderStatus.PAID);
                    order.getPayments().forEach(payment -> {
                        payment.setStatus(PaymentStatus.SUCCESS);
                        payment.setTransactionId(transactionId);
                        payment.setPaidAt(now);
                    });
                });
                clearCartItemsForOrders(group.getUser().getId(), orders);
                orderRepository.saveAll(orders);
            } else {
                applyTerminalFailure(group, targetStatus, OrderStatus.PAYMENT_FAILED);
            }
            paymentGroupRepository.save(group);
        }
        paymentCallbackRepository.save(PaymentCallback.builder()
                .provider(provider)
                .eventKey(eventKey)
                .paymentCode(group.getPaymentCode())
                .status(targetStatus.name())
                .rawPayload(toJson(rawPayload))
                .build());
        return toPaymentGroupResponse(group);
    }

    private void applyTerminalFailure(PaymentGroup group, PaymentStatus paymentStatus, OrderStatus orderStatus) {
        if (group.getStatus() != PaymentStatus.PENDING) {
            return;
        }
        group.setStatus(paymentStatus);
        List<Order> orders = orderRepository.findByPaymentsPaymentGroupId(group.getId());
        restoreStockForOrders(orders);
        orders.forEach(order -> {
            order.setOrderStatus(orderStatus);
            order.getPayments().forEach(payment -> payment.setStatus(paymentStatus));
        });
        orderRepository.saveAll(orders);
        paymentGroupRepository.save(group);
    }

    private void restoreStockForOrders(List<Order> orders) {
        Map<Long, Integer> quantities = orderVariationQuantities(orders);
        Map<Long, ProductVariation> variations = lockedVariations(quantities.keySet());
        quantities.forEach((variationId, quantity) -> {
            ProductVariation variation = variations.get(variationId);
            if (variation != null) {
                Product product = variation.getProduct();
                variation.setStockQuantity(variation.getStockQuantity() + quantity);
                product.setStockQuantity(product.getStockQuantity() + quantity);
            }
        });
    }

    private void deductStockForOrders(List<Order> orders) {
        Map<Long, Integer> quantities = orderVariationQuantities(orders);
        Map<Long, ProductVariation> variations = lockedVariations(quantities.keySet());
        quantities.forEach((variationId, quantity) -> {
            ProductVariation variation = variations.get(variationId);
            if (variation == null || variation.getStockQuantity() < quantity) {
                throw new AppException(CartErrorCode.CART_STOCK_NOT_ENOUGH);
            }
            Product product = variation.getProduct();
            variation.setStockQuantity(variation.getStockQuantity() - quantity);
            product.setStockQuantity(Math.max(0, product.getStockQuantity() - quantity));
        });
    }

    private void clearCartItemsForOrders(String userId, List<Order> orders) {
        Set<Long> variationIds = orderVariationQuantities(orders).keySet();
        if (!variationIds.isEmpty()) {
            cartItemRepository.deleteActiveCartItemsByUserIdAndVariationIds(userId, variationIds);
        }
    }

    private Map<Long, Integer> orderVariationQuantities(List<Order> orders) {
        return orders.stream()
                .flatMap(order -> order.getItems().stream())
                .filter(item -> item.getProductVariationId() != null)
                .collect(Collectors.toMap(
                        OrderItem::getProductVariationId, OrderItem::getQuantity, Integer::sum, TreeMap::new));
    }

    private Map<Long, ProductVariation> lockedVariations(Collection<Long> variationIds) {
        if (variationIds.isEmpty()) {
            return Map.of();
        }
        return variationRepository.findAllByIdInForUpdate(variationIds).stream()
                .collect(Collectors.toMap(ProductVariation::getId, Function.identity()));
    }

    private PaymentProviderClient provider(PaymentMethod method) {
        return paymentProviderClients.stream()
                .filter(client -> client.method() == method)
                .findFirst()
                .orElseThrow(() -> new AppException(PaymentErrorCode.PAYMENT_PROVIDER_ERROR));
    }

    private PaymentGroupResponse toPaymentGroupResponse(PaymentGroup group) {
        List<Order> orders = orderRepository.findByPaymentsPaymentGroupId(group.getId());
        return commerceMapper.toPaymentGroupResponse(group, orders);
    }

    private void applyProviderResult(PaymentGroup group, PaymentProviderResult result) {
        group.setProviderTxnRef(result.providerTxnRef());
        group.setProviderTransactionId(result.providerTransactionId());
        group.setPaymentUrl(result.paymentUrl());
        group.setDeeplink(result.deeplink());
        group.setQrCodeUrl(result.qrCodeUrl());
        group.setRawResponse(result.rawResponse());
    }

    private String toJson(Map<String, ?> payload) {
        return payload.toString();
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
