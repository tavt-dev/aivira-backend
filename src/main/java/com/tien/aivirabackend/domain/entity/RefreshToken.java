package com.tien.aivirabackend.domain.entity;

import java.time.Instant;

import jakarta.persistence.*;

import com.tien.aivirabackend.constant.RevocationReason;
import com.tien.aivirabackend.domain.entity.user.User;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "refresh_tokens")
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RefreshToken extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, unique = true, length = 36)
    String id;

    @Column(name = "token_hash", nullable = false, length = 64)
    String tokenHash; // SHA-256 hex

    @Column(nullable = false, unique = true, length = 36)
    String jti;

    @Column(name = "family_id", nullable = false, length = 36)
    String familyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(name = "issued_at", nullable = false)
    Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;

    @Column(name = "replaced_by", length = 36)
    String replacedBy;

    @Column(name = "last_used_at")
    Instant lastUsedAt;

    @Column(name = "device_info", length = 512)
    String deviceInfo;

    @Column(name = "ip_address", length = 45)
    String ipAddress;

    @Builder.Default
    @Column(nullable = false)
    boolean revoked = false;

    @Column(name = "revoked_at")
    Instant revokedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "revocation_reason", length = 100)
    RevocationReason revocationReason;

    public boolean isValid() {
        return !revoked && Instant.now().isBefore(expiresAt);
    }

    public void markUsed(Instant now) {
        this.lastUsedAt = now;
    }

    public void revoke(RevocationReason reason) {
        this.revoked = true;
        this.revokedAt = Instant.now();
        this.revocationReason = reason;
    }
}
