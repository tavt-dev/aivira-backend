package com.tien.aivirabackend.repository;

import java.time.Instant;
import java.util.List;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tien.aivirabackend.domain.entity.notification.NotificationOutbox;
import com.tien.aivirabackend.constant.NotificationOutboxStatus;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {
    boolean existsByEventKey(String eventKey);

    long countByStatus(NotificationOutboxStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.QueryHints(
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("""
            select o from NotificationOutbox o
            where o.status in (com.tien.aivirabackend.constant.NotificationOutboxStatus.PENDING,
                com.tien.aivirabackend.constant.NotificationOutboxStatus.RETRY)
              and o.nextAttemptAt <= :now order by o.id
            """)
    List<NotificationOutbox> lockDispatchBatch(@Param("now") Instant now, Pageable pageable);

    @Modifying
    @Query("""
            update NotificationOutbox o set o.status = com.tien.aivirabackend.constant.NotificationOutboxStatus.RETRY,
            o.lockedAt = null, o.nextAttemptAt = :now
            where o.status = com.tien.aivirabackend.constant.NotificationOutboxStatus.PROCESSING
              and o.lockedAt < :cutoff
            """)
    int recoverStale(@Param("cutoff") Instant cutoff, @Param("now") Instant now);

    @Modifying
    @Query(value = """
            delete from notification_outbox where status = 'COMPLETED' and processed_at < :cutoff limit 500
            """, nativeQuery = true)
    int deleteCompletedBefore(@Param("cutoff") Instant cutoff);
}
