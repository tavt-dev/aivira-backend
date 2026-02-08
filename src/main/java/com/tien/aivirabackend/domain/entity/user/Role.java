package com.tien.aivirabackend.domain.entity.user;

import java.util.Set;

import jakarta.persistence.*;

import com.tien.aivirabackend.constant.PredefinedRole;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "roles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    PredefinedRole code;

    @Column(length = 255)
    String description;

    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    Set<User> users;
}
