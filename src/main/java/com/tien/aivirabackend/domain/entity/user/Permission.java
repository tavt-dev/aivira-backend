package com.tien.aivirabackend.domain.entity.user;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;

import com.tien.aivirabackend.constant.PermissionCode;
import com.tien.aivirabackend.constant.PermissionGroup;
import com.tien.aivirabackend.domain.entity.BaseEntity;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Permission extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 100)
    PermissionCode code;

    @Column(nullable = false, length = 150)
    String name;

    @Column(length = 500)
    String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_group", nullable = false, length = 50)
    PermissionGroup group;

    @Builder.Default
    @Column(name = "is_system", nullable = false)
    Boolean system = true;

    @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)
    @Builder.Default
    Set<Role> roles = new HashSet<>();
}
