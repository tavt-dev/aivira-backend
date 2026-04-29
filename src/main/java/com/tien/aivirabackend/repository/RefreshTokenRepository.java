package com.tien.aivirabackend.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tien.aivirabackend.constant.RevocationReason;
import com.tien.aivirabackend.domain.entity.RefreshToken;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByJti(String jti);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Query("SELECT t FROM RefreshToken t WHERE t.user.id = :userId "
            + "AND t.revoked = false AND t.expiresAt > :now ORDER BY t.issuedAt DESC")
    List<RefreshToken> findActiveTokensByUserId(@Param("userId") String userId, @Param("now") Instant now);

    List<RefreshToken> findByFamilyId(String familyId);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true, t.revokedAt = :now, "
            + "t.revocationReason = :reason WHERE t.familyId = :familyId AND t.revoked = false")
    int revokeAllByFamilyId(
            @Param("familyId") String familyId, @Param("now") Instant now, @Param("reason") RevocationReason reason);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END "
            + "FROM RefreshToken t WHERE t.jti = :jti AND t.revoked = false AND t.expiresAt > :now")
    boolean existsValidTokenByJti(@Param("jti") String jti, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true, t.revokedAt = :now, "
            + "t.revocationReason = :reason WHERE t.user.id = :userId AND t.revoked = false")
    int revokeAllByUserId(
            @Param("userId") String userId, @Param("now") Instant now, @Param("reason") RevocationReason reason);

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :cutoffTime")
    int deleteExpiredTokens(@Param("cutoffTime") Instant cutoffTime);

    @Query("SELECT COUNT(t) FROM RefreshToken t WHERE t.user.id = :userId "
            + "AND t.revoked = false AND t.expiresAt > :now")
    long countActiveSessionsByUserId(@Param("userId") String userId, @Param("now") Instant now);
}
