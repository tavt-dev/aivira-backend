package com.tien.aivirabackend.domain.entity.user;

import com.tien.aivirabackend.constant.OtpType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

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
