package com.tien.aivirabackend.service.commerce;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.tien.aivirabackend.config.properties.PaymentProperties;
import com.tien.aivirabackend.constant.OrderStatus;
import com.tien.aivirabackend.constant.PaymentMethod;
import com.tien.aivirabackend.constant.PaymentStatus;
import com.tien.aivirabackend.domain.dto.RequestMetadata;
import com.tien.aivirabackend.domain.dto.request.CheckoutRequest;
import com.tien.aivirabackend.domain.dto.response.CheckoutPreviewResponse;
import com.tien.aivirabackend.domain.dto.response.CheckoutResponse;
import com.tien.aivirabackend.domain.entity.catalog.Product;
import com.tien.aivirabackend.domain.entity.catalog.ProductVariation;
import com.tien.aivirabackend.domain.entity.transaction.CartItem;
import com.tien.aivirabackend.domain.entity.transaction.Order;
import com.tien.aivirabackend.domain.entity.transaction.OrderItem;
import com.tien.aivirabackend.domain.entity.transaction.payment.Payment;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentAttempt;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentGroup;
import com.tien.aivirabackend.domain.entity.user.Address;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.mapper.CommerceMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.AddressErrorCode;
import com.tien.aivirabackend.exception.errorCode.CheckoutErrorCode;
import com.tien.aivirabackend.repository.AddressRepository;
import com.tien.aivirabackend.repository.CartItemRepository;
import com.tien.aivirabackend.repository.OrderRepository;
import com.tien.aivirabackend.repository.PaymentAttemptRepository;
import com.tien.aivirabackend.repository.PaymentGroupRepository;
import com.tien.aivirabackend.service.auth.CurrentUserService;
import com.tien.aivirabackend.service.discount.DiscountCalculation;
import com.tien.aivirabackend.service.discount.DiscountItem;
import com.tien.aivirabackend.service.discount.DiscountService;
import com.tien.aivirabackend.service.payment.PaymentProviderSupportService;
import com.tien.aivirabackend.service.payment.provider.PaymentProviderClient;
import com.tien.aivirabackend.service.payment.provider.PaymentProviderResult;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CheckoutServiceImpl implements CheckoutService {
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    CartItemRepository cartItemRepository;
    AddressRepository addressRepository;
    OrderRepository orderRepository;
    PaymentGroupRepository paymentGroupRepository;
    PaymentAttemptRepository paymentAttemptRepository;
    PaymentProperties paymentProperties;
    CurrentUserService currentUserService;
    CommerceMapper commerceMapper;
    PaymentProviderSupportService paymentProviderSupportService;
    InventoryService inventoryService;
    DiscountService discountService;

    @Override
    @Transactional(readOnly = true)
    public CheckoutPreviewResponse preview(CheckoutRequest request) {
        User user = currentUserService.getCurrentUser();
        validatePaymentMethod(request.getPaymentMethod());
        CheckoutContext context = loadCheckoutContext(user, request, false);
        DiscountCalculation calculation = discountService.calculate(user, context.cartItems(), context.variations(),
                request.getCouponCode());
        return discountService.toPreviewResponse(calculation);
    }

    @Override
    @Transactional
    public CheckoutResponse checkout(CheckoutRequest request, RequestMetadata requestMetadata) {
        User user = currentUserService.getCurrentUser();
        validatePaymentMethod(request.getPaymentMethod());
        CheckoutContext context = loadCheckoutContext(user, request, true);
        DiscountCalculation calculation = discountService.calculate(user, context.cartItems(), context.variations(),
                request.getCouponCode());

        PaymentGroup paymentGroup = PaymentGroup.builder().paymentCode(generatePaymentCode()).user(user)
                .method(request.getPaymentMethod()).status(PaymentStatus.PENDING).amount(ZERO)
                .expiresAt(Instant.now().plusSeconds(paymentProperties.getPendingTtlMinutes() * 60)).build();

        List<Order> orders = new ArrayList<>();
        Order checkoutOrder = buildOrder(user, context.address(), request, calculation);
        orders.add(checkoutOrder);
        paymentGroup.setAmount(checkoutOrder.getTotalAmount());

        PaymentGroup savedPaymentGroup = paymentGroupRepository.save(paymentGroup);
        OrderStatus initialOrderStatus = request.getPaymentMethod() == PaymentMethod.COD
                ? OrderStatus.PENDING_CONFIRMATION : OrderStatus.PENDING_PAYMENT;
        for (Order order : orders) {
            order.setOrderStatus(initialOrderStatus);
            Payment payment = Payment.builder().order(order).paymentGroup(savedPaymentGroup)
                    .method(request.getPaymentMethod()).status(PaymentStatus.PENDING).amount(order.getTotalAmount())
                    .build();
            order.getPayments().add(payment);
            savedPaymentGroup.getPayments().add(payment);
        }

        inventoryService.deductCartItems(context.cartItems(), context.variations());
        List<Order> savedOrders = orderRepository.saveAll(orders);
        discountService.reserveOrFinalizeCoupon(user, savedOrders.getFirst(), calculation,
                request.getPaymentMethod() == PaymentMethod.COD);
        PaymentAttempt attempt = paymentProviderSupportService.createAttempt(savedPaymentGroup);
        PaymentProviderResult providerResult = paymentProviderSupportService.createPaymentWithMetrics(
                provider(request.getPaymentMethod()), savedPaymentGroup, attempt, requestMetadata);
        paymentProviderSupportService.applyProviderResult(savedPaymentGroup, attempt, providerResult);
        paymentGroupRepository.save(savedPaymentGroup);
        paymentAttemptRepository.save(attempt);

        if (request.getPaymentMethod() == PaymentMethod.COD) {
            cartItemRepository.deleteAll(context.cartItems());
        }

        return CheckoutResponse.builder().paymentGroupCode(savedPaymentGroup.getPaymentCode())
                .paymentMethod(savedPaymentGroup.getMethod()).paymentStatus(savedPaymentGroup.getStatus())
                .totalAmount(savedPaymentGroup.getAmount()).paymentUrl(savedPaymentGroup.getPaymentUrl())
                .deeplink(savedPaymentGroup.getDeeplink()).qrCodeUrl(savedPaymentGroup.getQrCodeUrl())
                .expiresAt(savedPaymentGroup.getExpiresAt())
                .orders(savedOrders.stream().map(commerceMapper::toOrderResponse).toList()).build();
    }

    private Order buildOrder(User user, Address address, CheckoutRequest request, DiscountCalculation calculation) {
        Order order = Order.builder().orderCode(generateOrderCode()).user(user).shippingAddress(address)
                .shippingRecipientName(address.getRecipientName()).shippingPhoneNumber(address.getPhoneNumber())
                .shippingAddressLine(address.getAddressLine()).shippingWard(address.getWard())
                .shippingDistrict(address.getDistrict()).shippingCity(address.getCity()).shippingFee(ZERO)
                .discountAmount(calculation.discountAmount()).couponCode(calculation.couponCode())
                .notes(trimToNull(request.getNotes())).build();
        for (DiscountItem discountItem : calculation.items()) {
            CartItem cartItem = discountItem.cartItem();
            ProductVariation variation = discountItem.variation();
            Product product = variation.getProduct();
            BigDecimal additionalPrice = nullToZero(variation.getAdditionalPrice());
            OrderItem orderItem = OrderItem.builder().order(order).productId(product.getId())
                    .productVariationId(variation.getId()).productName(product.getProductName()).sku(variation.getSku())
                    .variationColor(variation.getColor()).variationSize(variation.getSize())
                    .thumbnailUrl(StringUtils.hasText(variation.getImageUrl()) ? variation.getImageUrl()
                            : product.getThumbnailUrl())
                    .basePrice(product.getPrice()).additionalPrice(additionalPrice)
                    .discountAmount(discountItem.promotionDiscountAmount()).finalPrice(discountItem.unitPrice())
                    .promotionName(discountItem.promotionName()).quantity(cartItem.getQuantity()).build();
            order.getItems().add(orderItem);
        }
        order.setSubtotal(calculation.subtotal());
        order.setTotalAmount(calculation.totalAmount());
        return order;
    }

    private CheckoutContext loadCheckoutContext(User user, CheckoutRequest request, boolean lockInventory) {
        Address address = addressRepository.findByIdAndUserIdAndActiveTrue(request.getAddressId(), user.getId())
                .orElseThrow(() -> new AppException(AddressErrorCode.ADDRESS_NOT_FOUND));
        List<Long> selectedIds = request.getCartItemIds().stream().distinct().sorted().toList();
        if (selectedIds.isEmpty()) {
            throw new AppException(CheckoutErrorCode.CHECKOUT_EMPTY_ITEMS);
        }
        List<CartItem> cartItems = cartItemRepository.findByIdInAndCartUserIdAndCartActiveTrue(selectedIds,
                user.getId());
        if (cartItems.size() != selectedIds.size()) {
            throw new AppException(CheckoutErrorCode.CHECKOUT_CART_ITEM_MISMATCH);
        }
        Map<Long, ProductVariation> variations = lockInventory ? inventoryService.lockVariationsForCartItems(cartItems)
                : cartItems.stream().collect(java.util.stream.Collectors
                        .toMap(item -> item.getProductVariation().getId(), CartItem::getProductVariation));
        inventoryService.validateCheckoutItems(cartItems, variations);
        return new CheckoutContext(address, cartItems, variations);
    }

    private PaymentProviderClient provider(PaymentMethod method) {
        return paymentProviderSupportService.provider(method,
                () -> new AppException(CheckoutErrorCode.CHECKOUT_PAYMENT_METHOD_UNSUPPORTED));
    }

    private void validatePaymentMethod(PaymentMethod method) {
        if (method == null
                || (method != PaymentMethod.COD && method != PaymentMethod.VNPAY && method != PaymentMethod.MOMO)) {
            throw new AppException(CheckoutErrorCode.CHECKOUT_PAYMENT_METHOD_UNSUPPORTED);
        }
    }

    private String generatePaymentCode() {
        String code;
        do {
            code = "PAY" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (paymentGroupRepository.existsByPaymentCode(code));
        return code;
    }

    private String generateOrderCode() {
        String code;
        do {
            code = "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (orderRepository.existsByOrderCode(code));
        return code;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record CheckoutContext(Address address, List<CartItem> cartItems, Map<Long, ProductVariation> variations) {
    }
}
