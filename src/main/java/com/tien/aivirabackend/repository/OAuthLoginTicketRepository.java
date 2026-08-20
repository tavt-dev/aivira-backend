package com.tien.aivirabackend.repository;

import java.time.Instant;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tien.aivirabackend.domain.entity.user.OAuthLoginTicket;

public interface OAuthLoginTicketRepository extends JpaRepository<OAuthLoginTicket, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = { "user", "user.roles" })
    @Query("select t from OAuthLoginTicket t where t.ticketHash = :ticketHash")
    Optional<OAuthLoginTicket> findByTicketHashForUpdate(@Param("ticketHash") String ticketHash);

    @Modifying
    @Query("delete from OAuthLoginTicket t where t.expiresAt < :now or t.consumedAt < :usedBefore")
    int deleteExpiredOrConsumedBefore(@Param("now") Instant now, @Param("usedBefore") Instant usedBefore);
}
