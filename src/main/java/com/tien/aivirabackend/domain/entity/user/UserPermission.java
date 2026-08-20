package com.tien.aivirabackend.domain.entity.user;

import java.time.Instant;

import jakarta.persistence.*;

import com.tien.aivirabackend.domain.entity.BaseEntity;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "user_permissions", uniqueConstraints = @UniqueConstraint(name = "uk_user_permission_active", columnNames = {
        "user_id", "permission_id", "is_active" }))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserPermission extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    Permission permission;

    @Column(length = 500)
    String reason;

    @Column(name = "expires_at")
    Instant expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by")
    User grantedBy;

    @Column(name = "granted_at", nullable = false)
    Instant grantedAt;

    @Column(name = "revoked_at")
    Instant revokedAt;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    Boolean active = true;

    public boolean isCurrentlyActive(Instant now) {
        return Boolean.TRUE.equals(active) && revokedAt == null && (expiresAt == null || expiresAt.isAfter(now));
    }

    public void revoke(Instant revokedAt) {
        this.active = false;
        this.revokedAt = revokedAt;
    }
}
