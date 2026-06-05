package com.tien.aivirabackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tien.aivirabackend.domain.entity.transaction.Refund;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {
    boolean existsByRefundCode(String refundCode);

    boolean existsByOrder_Id(Long orderId);
}
