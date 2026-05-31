package com.tien.aivirabackend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tien.aivirabackend.constant.OrderStatus;
import com.tien.aivirabackend.domain.entity.transaction.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    boolean existsByOrderCode(String orderCode);

    @EntityGraph(attributePaths = {"payments", "payments.paymentGroup"})
    Page<Order> findByUserId(String userId, Pageable pageable);

    @EntityGraph(attributePaths = {"payments", "payments.paymentGroup"})
    Page<Order> findByUserIdAndOrderStatus(String userId, OrderStatus orderStatus, Pageable pageable);

    @EntityGraph(attributePaths = {"items"})
    List<Order> findByPaymentsPaymentGroupId(Long paymentGroupId);

    @EntityGraph(attributePaths = {"items"})
    List<Order> findByIdIn(Collection<Long> ids);

    @EntityGraph(attributePaths = {"items"})
    Optional<Order> findDetailedById(Long id);

    @EntityGraph(attributePaths = {"items"})
    Optional<Order> findDetailedByIdAndUserId(Long id, String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id and o.user.id = :userId")
    Optional<Order> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") String userId);

    long countByPaymentsPaymentGroupId(Long paymentGroupId);
}
