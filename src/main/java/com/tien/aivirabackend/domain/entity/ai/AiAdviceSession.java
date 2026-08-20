package com.tien.aivirabackend.domain.entity.ai;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

import com.tien.aivirabackend.domain.entity.BaseEntity;
import com.tien.aivirabackend.domain.entity.user.User;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "ai_advice_sessions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiAdviceSession extends BaseEntity {
    @Id
    @Column(length = 36)
    String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    User user;

    @Column(name = "guest_key", length = 36)
    String guestKey;

    @Column(nullable = false, length = 10)
    String locale;

    @Column(name = "personalization_enabled", nullable = false)
    boolean personalizationEnabled;

    @Column(name = "last_activity_at", nullable = false)
    Instant lastActivityAt;

    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<AiAdviceMessage> messages = new ArrayList<>();
}
