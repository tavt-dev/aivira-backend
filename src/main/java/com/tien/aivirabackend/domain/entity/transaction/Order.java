package com.tien.aivirabackend.domain.entity.transaction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

import com.tien.aivirabackend.constant.OrderStatus;
import com.tien.aivirabackend.domain.entity.BaseEntity;
import com.tien.aivirabackend.domain.entity.marketplace.Shop;
import com.tien.aivirabackend.domain.entity.transaction.payment.Payment;
import com.tien.aivirabackend.domain.entity.user.Address;
import com.tien.aivirabackend.domain.entity.user.User;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Order extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "order_code", nullable = false, unique = true, length = 50)
    String orderCode; // mã đơn hiển thị cho user

    @Column(name = "subtotal", nullable = false, precision = 19, scale = 2)
    BigDecimal subtotal;

    @Column(name = "shipping_fee", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    BigDecimal totalAmount;

    @Column(name = "coupon_code", length = 50)
    String couponCode;

    @Column(length = 500)
    String notes;

    @Column(name = "cancel_reason", length = 500)
    String cancelReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    OrderStatus orderStatus = OrderStatus.PENDING_PAYMENT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    Address shippingAddress;

    @Column(name = "shipping_recipient_name", nullable = false)
    String shippingRecipientName;

    @Column(name = "shipping_phone_number", nullable = false)
    String shippingPhoneNumber;

    @Column(name = "shipping_address_line", length = 500, nullable = false)
    String shippingAddressLine;

    @Column(name = "shipping_ward")
    String shippingWard;

    @Column(name = "shipping_district")
    String shippingDistrict;

    @Column(name = "shipping_city")
    String shippingCity;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<Payment> payments = new ArrayList<>();
}
