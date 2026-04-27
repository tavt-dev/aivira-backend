package com.tien.aivirabackend.service;

import com.tien.aivirabackend.domain.entity.marketplace.Shop;

public interface ShopOwnershipService {
    Shop getCurrentUserShop();

    Shop requireCurrentUserApprovedShop();

    void requireOwner(Shop shop, String userId);
}
