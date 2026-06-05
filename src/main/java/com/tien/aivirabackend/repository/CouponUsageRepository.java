package com.tien.aivirabackend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tien.aivirabackend.constant.CouponUsageStatus;
import com.tien.aivirabackend.domain.entity.discount.CouponUsage;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {
    long countByCoupon_IdAndStatusIn(Long couponId, Collection<CouponUsageStatus> statuses);

    long countByCoupon_IdAndUser_IdAndStatusIn(
            Long couponId, String userId, Collection<CouponUsageStatus> statuses);

    Optional<CouponUsage> findByCoupon_IdAndOrder_Id(Long couponId, Long orderId);

    List<CouponUsage> findByOrder_IdInAndStatus(Collection<Long> orderIds, CouponUsageStatus status);
}
