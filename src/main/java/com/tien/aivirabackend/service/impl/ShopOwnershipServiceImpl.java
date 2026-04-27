package com.tien.aivirabackend.service.impl;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tien.aivirabackend.constant.ShopStatus;
import com.tien.aivirabackend.domain.entity.marketplace.Shop;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.AuthErrorCode;
import com.tien.aivirabackend.exception.errorCode.ShopErrorCode;
import com.tien.aivirabackend.repository.ShopRepository;
import com.tien.aivirabackend.service.ShopOwnershipService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service("shopOwnershipService")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShopOwnershipServiceImpl implements ShopOwnershipService {
    ShopRepository shopRepository;

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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(AuthErrorCode.AUTHENTICATION_FAILED);
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            throw new AppException(AuthErrorCode.AUTHENTICATION_FAILED);
        }

        String userId = jwt.getClaimAsString("user_id");
        if (userId == null || userId.isBlank()) {
            throw new AppException(AuthErrorCode.AUTHENTICATION_FAILED);
        }
        return userId;
    }
}
