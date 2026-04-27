package com.tien.aivirabackend.domain.mapper;

import org.springframework.stereotype.Component;

import com.tien.aivirabackend.domain.dto.response.ShopResponse;
import com.tien.aivirabackend.domain.entity.marketplace.Shop;
import com.tien.aivirabackend.domain.entity.user.User;

@Component
public class ShopMapper {
    public ShopResponse toShopResponse(Shop shop) {
        if (shop == null) {
            return null;
        }

        User owner = shop.getOwner();
        return ShopResponse.builder()
                .id(shop.getId())
                .ownerId(owner == null ? null : owner.getId())
                .ownerUsername(owner == null ? null : owner.getUsername())
                .ownerEmail(owner == null ? null : owner.getEmail())
                .shopName(shop.getShopName())
                .slug(shop.getSlug())
                .logoUrl(shop.getLogoUrl())
                .description(shop.getDescription())
                .businessEmail(shop.getBusinessEmail())
                .phoneNumber(shop.getPhoneNumber())
                .legalName(shop.getLegalName())
                .taxCode(shop.getTaxCode())
                .pickupAddressLine(shop.getPickupAddressLine())
                .pickupWard(shop.getPickupWard())
                .pickupDistrict(shop.getPickupDistrict())
                .pickupCity(shop.getPickupCity())
                .status(shop.getStatus())
                .rejectionReason(shop.getRejectionReason())
                .lockedReason(shop.getLockedReason())
                .approvedBy(shop.getApprovedBy())
                .approvedAt(shop.getApprovedAt())
                .rejectedBy(shop.getRejectedBy())
                .rejectedAt(shop.getRejectedAt())
                .lockedBy(shop.getLockedBy())
                .lockedAt(shop.getLockedAt())
                .createdAt(shop.getCreatedAt())
                .updatedAt(shop.getUpdatedAt())
                .build();
    }
}
