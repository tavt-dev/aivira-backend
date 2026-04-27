package com.tien.aivirabackend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tien.aivirabackend.constant.PredefinedRole;
import com.tien.aivirabackend.domain.entity.user.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByCode(PredefinedRole code);

    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findWithPermissionsByCode(PredefinedRole code);

    @EntityGraph(attributePaths = "permissions")
    java.util.List<Role> findAllByOrderByCodeAsc();
}
