package com.tien.aivirabackend.domain.dto.response;

import com.tien.aivirabackend.constant.ShopStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SellerDashboardResponse {
    Long shopId;
    String shopName;
    ShopStatus shopStatus;
    long totalOrders;
    long pendingOrders;
    long totalProducts;
    long lowStockProducts;
    long revenue;
}
