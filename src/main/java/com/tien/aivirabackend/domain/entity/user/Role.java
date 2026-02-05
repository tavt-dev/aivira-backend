package com.tien.aivirabackend.domain.entity.user;

import com.tien.aivirabackend.constant.PredefinedRole;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;
import java.util.UUID;

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
    @GeneratedValue(strategy =  GenerationType.IDENTITY)
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

