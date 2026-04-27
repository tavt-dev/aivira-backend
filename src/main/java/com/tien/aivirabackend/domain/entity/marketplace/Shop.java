package com.tien.aivirabackend.domain.entity.marketplace;

import java.time.Instant;

import jakarta.persistence.*;

import com.tien.aivirabackend.constant.ShopStatus;
import com.tien.aivirabackend.domain.entity.BaseEntity;
import com.tien.aivirabackend.domain.entity.user.User;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(
        name = "shops",
        indexes = {
            @Index(name = "idx_shops_status", columnList = "status"),
            @Index(name = "idx_shops_shop_name", columnList = "shop_name")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Shop extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, unique = true)
    User owner;

    @Column(name = "shop_name", nullable = false, length = 150)
    String shopName;

    @Column(nullable = false, unique = true, length = 180)
    String slug;

    @Column(name = "logo_url")
    String logoUrl;

    @Column(name = "logo_public_id")
    String logoPublicId;

    @Column(length = 1000)
    String description;

    @Column(name = "business_email", nullable = false, length = 120)
    String businessEmail;

    @Column(name = "phone_number", nullable = false, length = 20)
    String phoneNumber;

    @Column(name = "legal_name", nullable = false, length = 150)
    String legalName;

    @Column(name = "tax_code", length = 50)
    String taxCode;

    @Column(name = "pickup_address_line", nullable = false, length = 500)
    String pickupAddressLine;

    @Column(name = "pickup_ward", length = 120)
    String pickupWard;

    @Column(name = "pickup_district", length = 120)
    String pickupDistrict;

    @Column(name = "pickup_city", nullable = false, length = 120)
    String pickupCity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    ShopStatus status;

    @Column(name = "rejection_reason", length = 500)
    String rejectionReason;

    @Column(name = "locked_reason", length = 500)
    String lockedReason;

    @Column(name = "approved_by")
    String approvedBy;

    @Column(name = "approved_at")
    Instant approvedAt;

    @Column(name = "rejected_by")
    String rejectedBy;

    @Column(name = "rejected_at")
    Instant rejectedAt;

    @Column(name = "locked_by")
    String lockedBy;

    @Column(name = "locked_at")
    Instant lockedAt;
}
