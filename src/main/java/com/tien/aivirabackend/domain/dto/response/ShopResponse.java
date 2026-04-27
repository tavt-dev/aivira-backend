package com.tien.aivirabackend.domain.dto.response;

import java.time.Instant;

import com.tien.aivirabackend.constant.ShopStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShopResponse {
    Long id;
    String ownerId;
    String ownerUsername;
    String ownerEmail;
    String shopName;
    String slug;
    String logoUrl;
    String description;
    String businessEmail;
    String phoneNumber;
    String legalName;
    String taxCode;
    String pickupAddressLine;
    String pickupWard;
    String pickupDistrict;
    String pickupCity;
    ShopStatus status;
    String rejectionReason;
    String lockedReason;
    String approvedBy;
    Instant approvedAt;
    String rejectedBy;
    Instant rejectedAt;
    String lockedBy;
    Instant lockedAt;
    Instant createdAt;
    Instant updatedAt;
}
