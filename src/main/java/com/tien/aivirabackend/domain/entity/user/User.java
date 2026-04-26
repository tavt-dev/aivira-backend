package com.tien.aivirabackend.domain.entity.user;

import java.util.ArrayList;
import java.util.HashSet;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import jakarta.persistence.*;

import com.tien.aivirabackend.constant.Gender;
import com.tien.aivirabackend.constant.SignInProvider;
import com.tien.aivirabackend.domain.entity.BaseEntity;
import com.tien.aivirabackend.domain.entity.review.Review;
import com.tien.aivirabackend.domain.entity.transaction.Cart;
import com.tien.aivirabackend.domain.entity.transaction.Order;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    String id;

    /* AUTH / LOGIN */
    @Column(nullable = false, unique = true, length = 50)
    String username;

    @Column(nullable = false, unique = true, length = 120)
    String email;

    @Column(length = 255)
    String password;

    @Enumerated(EnumType.STRING)
    SignInProvider provider;

    @Column(length = 128)
    String providerUserId;

    @Builder.Default
    @Column(nullable = false, name = "email_verified")
    Boolean emailVerified = false;

    /* PROFILE */
    @Column(name = "first_name", length = 50)
    String firstName;

    @Column(name = "last_name", length = 50)
    String lastName;

    @Enumerated(EnumType.STRING)
    Gender gender;

    @Column(name = "phone_number", length = 15)
    String phoneNumber;

    @Column(name = "avatar_url")
    String avatarUrl;

    @Column(name = "avatar_public_id")
    String avatarPublicId;

    /* STATUS */
    @Builder.Default
    @Column(nullable = false, name = "is_active")
    Boolean isActive = false;

    @Builder.Default
    Boolean isLocked = false;

    @Builder.Default
    Boolean isDeleted = false;

    @Builder.Default
    @Column(name = "token_version", nullable = false)
    Integer tokenVersion = 0;

    @Builder.Default
    @Column(name = "failed_login_attempts", nullable = false)
    Integer failedLoginAttempts = 0;

    @Column(name = "first_failed_login_at")
    Instant firstFailedLoginAt;

    @Column(name = "lockout_until")
    Instant lockoutUntil;

    /* RELATIONSHIPS */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Builder.Default
    Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    Set<Address> addresses = new HashSet<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    Cart cart;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    Set<Order> orders = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    List<Review> reviews = new ArrayList<>();
}
