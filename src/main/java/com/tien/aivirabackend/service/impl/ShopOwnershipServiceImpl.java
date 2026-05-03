package com.tien.aivirabackend.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tien.aivirabackend.constant.ShopStatus;
import com.tien.aivirabackend.domain.entity.marketplace.Shop;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.ShopErrorCode;
import com.tien.aivirabackend.repository.ShopRepository;
import com.tien.aivirabackend.service.CurrentUserService;
import com.tien.aivirabackend.service.ShopOwnershipService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service("shopOwnershipService")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShopOwnershipServiceImpl implements ShopOwnershipService {
    ShopRepository shopRepository;
    CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public Shop getCurrentUserShop() {
        return shopRepository
                .findWithOwnerByOwnerId(getCurrentUserId())
                .orElseThrow(() -> new AppException(ShopErrorCode.SHOP_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public Shop requireCurrentUserApprovedShop() {
        Shop shop = getCurrentUserShop();
        if (shop.getStatus() != ShopStatus.APPROVED) {
            throw new AppException(ShopErrorCode.SHOP_NOT_APPROVED);
        }
        return shop;
    }

    @Override
    public void requireOwner(Shop shop, String userId) {
        if (shop == null
                || shop.getOwner() == null
                || userId == null
                || !userId.equals(shop.getOwner().getId())) {
            throw new AppException(ShopErrorCode.SHOP_NOT_OWNER);
        }
    }

    private String getCurrentUserId() {
        return currentUserService.getCurrentUserId();
    }
}
