package com.tien.aivirabackend.service;

import org.springframework.web.multipart.MultipartFile;

import com.tien.aivirabackend.domain.dto.request.ApplyShopRequest;
import com.tien.aivirabackend.domain.dto.request.UpdateShopRequest;
import com.tien.aivirabackend.domain.dto.response.SellerDashboardResponse;
import com.tien.aivirabackend.domain.dto.response.ShopResponse;

public interface ShopService {
    ShopResponse apply(ApplyShopRequest request);

    ShopResponse getMyShop();

    ShopResponse updateMyShop(UpdateShopRequest request);

    ShopResponse resubmitMyShop();

    ShopResponse updateMyShopLogo(MultipartFile logoFile);

    SellerDashboardResponse getDashboard();
}
