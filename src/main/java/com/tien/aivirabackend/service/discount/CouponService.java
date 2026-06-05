package com.tien.aivirabackend.service.discount;

import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.CouponCreateRequest;
import com.tien.aivirabackend.domain.dto.request.CouponUpdateRequest;
import com.tien.aivirabackend.domain.dto.response.CouponResponse;

public interface CouponService {
    PageResponse<CouponResponse> getCoupons(int page, int size);

    CouponResponse getCoupon(Long couponId);

    CouponResponse createCoupon(CouponCreateRequest request);

    CouponResponse updateCoupon(Long couponId, CouponUpdateRequest request);

    void deleteCoupon(Long couponId);
}
