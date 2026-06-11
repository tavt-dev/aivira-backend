package com.tien.aivirabackend.repository;

import java.time.Instant;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tien.aivirabackend.domain.entity.user.OAuthLoginState;

public interface OAuthLoginStateRepository extends JpaRepository<OAuthLoginState, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from OAuthLoginState s where s.stateHash = :stateHash")
    Optional<OAuthLoginState> findByStateHashForUpdate(@Param("stateHash") String stateHash);

    @Modifying
    @Query("delete from OAuthLoginState s where s.expiresAt < :now or s.consumedAt < :usedBefore")
    int deleteExpiredOrConsumedBefore(@Param("now") Instant now, @Param("usedBefore") Instant usedBefore);
}
