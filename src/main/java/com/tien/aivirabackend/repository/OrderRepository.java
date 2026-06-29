package com.tien.aivirabackend.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tien.aivirabackend.constant.OrderStatus;
import com.tien.aivirabackend.domain.entity.transaction.Order;
import com.tien.aivirabackend.repository.projection.DailyOrderCountProjection;
import com.tien.aivirabackend.repository.projection.OrderStatusCountProjection;
import com.tien.aivirabackend.repository.projection.TopBookProjection;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    boolean existsByOrderCode(String orderCode);

    @EntityGraph(attributePaths = {"payments", "payments.paymentGroup"})
    Page<Order> findByUserId(String userId, Pageable pageable);

    @EntityGraph(attributePaths = {"payments", "payments.paymentGroup"})
    Page<Order> findByUserIdAndOrderStatus(String userId, OrderStatus orderStatus, Pageable pageable);

    @EntityGraph(attributePaths = {"items"})
    List<Order> findByPaymentsPaymentGroupId(Long paymentGroupId);

    @EntityGraph(attributePaths = {"items"})
    List<Order> findByIdIn(Collection<Long> ids);

    @EntityGraph(attributePaths = {"items", "refund"})
    Optional<Order> findWithItemsAndRefundById(Long id);

    @EntityGraph(attributePaths = {"items", "refund"})
    Optional<Order> findWithItemsAndRefundByIdAndUserId(Long id, String userId);

    @EntityGraph(attributePaths = {"payments", "payments.paymentGroup"})
    Optional<Order> findWithPaymentsById(Long id);

    @EntityGraph(attributePaths = {"payments", "payments.paymentGroup"})
    Optional<Order> findWithPaymentsByIdAndUserId(Long id, String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id and o.user.id = :userId")
    Optional<Order> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") String userId);

    long countByPaymentsPaymentGroupId(Long paymentGroupId);

    long countByCreatedAtBetween(Instant fromDate, Instant toDate);

    long countByOrderStatusIn(Collection<OrderStatus> statuses);

    @Query(
            """
			select o.orderStatus as status, count(o.id) as count
			from Order o
			where o.createdAt between :fromDate and :toDate
			group by o.orderStatus
			""")
    List<OrderStatusCountProjection> countOrdersByStatusBetween(
            @Param("fromDate") Instant fromDate, @Param("toDate") Instant toDate);

    @Query(
            value =
                    """
					select date(o.created_at) as orderDate,
						count(o.id) as orderCount
					from orders o
					where o.created_at between :fromDate and :toDate
					group by date(o.created_at)
					order by orderDate asc
					""",
            nativeQuery = true)
    List<DailyOrderCountProjection> countDailyOrdersBetween(
            @Param("fromDate") Instant fromDate, @Param("toDate") Instant toDate);

    @Query(
            """
			select oi.productId as productId,
				max(oi.productName) as productName,
				max(oi.sku) as sku,
				max(oi.thumbnailUrl) as thumbnailUrl,
				sum(oi.quantity) as quantitySold,
				coalesce(sum(oi.finalPrice * oi.quantity), 0) as revenue
			from OrderItem oi
			join oi.order o
			where o.orderStatus not in :excludedStatuses
			and o.createdAt between :fromDate and :toDate
			group by oi.productId
			order by sum(oi.quantity) desc, max(oi.productName) asc
			""")
    List<TopBookProjection> findTopBooksBetween(
            @Param("excludedStatuses") Collection<OrderStatus> excludedStatuses,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable);
}
