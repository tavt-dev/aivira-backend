package com.tien.aivirabackend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tien.aivirabackend.domain.entity.transaction.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    boolean existsByOrderCode(String orderCode);

    @EntityGraph(attributePaths = {"items", "payments", "shop"})
    List<Order> findByPaymentsPaymentGroupId(Long paymentGroupId);

    @EntityGraph(attributePaths = {"items", "payments", "shop"})
    List<Order> findByIdIn(Collection<Long> ids);

    @EntityGraph(attributePaths = {"items", "payments", "shop"})
    Optional<Order> findDetailedById(Long id);
}
