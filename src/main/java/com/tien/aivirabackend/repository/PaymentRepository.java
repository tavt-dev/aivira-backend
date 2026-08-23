package com.tien.aivirabackend.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tien.aivirabackend.constant.PaymentStatus;
import com.tien.aivirabackend.domain.entity.transaction.payment.Payment;
import com.tien.aivirabackend.repository.projection.SalesPointProjection;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @EntityGraph(attributePaths = { "order", "paymentGroup" })
    Optional<Payment> findByIdAndOrderUserId(Long id, String userId);

    @EntityGraph(attributePaths = "paymentGroup")
    List<Payment> findByOrderIdInOrderByOrderIdAscIdAsc(Collection<Long> orderIds);

    long countByStatus(PaymentStatus status);

    long countByStatusInAndCreatedAtBetween(Collection<PaymentStatus> statuses, Instant fromDate, Instant toDate);

    @Query("""
            select coalesce(sum(p.amount), 0)
            from Payment p
            where p.status = com.tien.aivirabackend.constant.PaymentStatus.SUCCESS
            and ((p.paidAt is not null and p.paidAt between :fromDate and :toDate)
            	or (p.paidAt is null and p.createdAt between :fromDate and :toDate))
            """)
    BigDecimal sumSuccessfulRevenueBetween(@Param("fromDate") Instant fromDate, @Param("toDate") Instant toDate);

    @Query(value = """
            select date(coalesce(p.paid_at, p.created_at)) as salesDate,
            	coalesce(sum(p.amount), 0) as revenue
            from payments p
            where p.status = 'SUCCESS'
            and coalesce(p.paid_at, p.created_at) between :fromDate and :toDate
            group by date(coalesce(p.paid_at, p.created_at))
            order by salesDate asc
            """, nativeQuery = true)
    List<SalesPointProjection> aggregateDailySuccessfulRevenue(@Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate);
}
