package com.tien.aivirabackend.domain.entity.user;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.tien.aivirabackend.domain.entity.BaseEntity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "oauth_login_states")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OAuthLoginState extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "state_hash", nullable = false, unique = true, length = 64)
    String stateHash;

    @Column(name = "next_path", length = 500)
    String nextPath;

    @Column(name = "device_info", length = 512)
    String deviceInfo;

    @Column(name = "ip_address", length = 45)
    String ipAddress;

    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;

    @Column(name = "consumed_at")
    Instant consumedAt;

    public boolean isUsable(Instant now) {
        return consumedAt == null && expiresAt != null && expiresAt.isAfter(now);
    }
}
