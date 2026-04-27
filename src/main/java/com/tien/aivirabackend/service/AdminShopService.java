package com.tien.aivirabackend.service;

import com.tien.aivirabackend.constant.ShopStatus;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.ShopModerationRequest;
import com.tien.aivirabackend.domain.dto.response.ShopResponse;

public interface AdminShopService {
    PageResponse<ShopResponse> getShops(ShopStatus status, String keyword, int page, int size);

    ShopResponse getShop(Long shopId);

    ShopResponse approve(Long shopId);

    ShopResponse reject(Long shopId, ShopModerationRequest request);

    ShopResponse lock(Long shopId, ShopModerationRequest request);

    ShopResponse unlock(Long shopId);
}
