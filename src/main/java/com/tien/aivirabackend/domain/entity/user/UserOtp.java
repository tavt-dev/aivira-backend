package com.tien.aivirabackend.domain.entity.user;

import java.time.Instant;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.tien.aivirabackend.constant.OtpType;

import lombok.*;

@Entity
@Table(name = "user_otp")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOtp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(nullable = false, length = 6)
    String otpCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    OtpType otpType;

    @Column(name = "expires_time", nullable = false)
    Instant expiresTime;

    @Column(name = "used_at")
    Instant usedAt;

    @Column(nullable = false)
    boolean used;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
