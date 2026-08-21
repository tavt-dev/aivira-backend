package com.tien.aivirabackend.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tien.aivirabackend.constant.NotificationType;
import com.tien.aivirabackend.domain.entity.notification.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Query("""
            select n from Notification n
            where n.recipient.id = :userId
              and (:type is null or n.type = :type)
              and (:read is null or (:read = true and n.readAt is not null) or (:read = false and n.readAt is null))
            """)
    Page<Notification> findInbox(@Param("userId") String userId, @Param("read") Boolean read,
            @Param("type") NotificationType type, Pageable pageable);

    long countByRecipient_IdAndReadAtIsNull(String userId);

    Optional<Notification> findByIdAndRecipient_Id(Long id, String userId);

    @Modifying
    @Query("update Notification n set n.readAt = :readAt where n.recipient.id = :userId and n.readAt is null")
    int markAllRead(@Param("userId") String userId, @Param("readAt") Instant readAt);

    @Modifying
    @Query(value = """
            delete from notifications where read_at is not null and read_at < :cutoff
              and not exists (select 1 from notification_outbox o where o.notification_id = notifications.id)
            limit 500
            """, nativeQuery = true)
    int deleteReadBefore(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query(value = """
            delete from notifications where read_at is null and created_at < :cutoff
              and not exists (select 1 from notification_outbox o where o.notification_id = notifications.id)
            limit 500
            """, nativeQuery = true)
    int deleteUnreadBefore(@Param("cutoff") Instant cutoff);
}
