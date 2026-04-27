package com.tien.aivirabackend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tien.aivirabackend.constant.PermissionCode;
import com.tien.aivirabackend.domain.entity.user.Permission;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByCode(PermissionCode code);

    List<Permission> findByCodeIn(Collection<PermissionCode> codes);

    boolean existsByCode(PermissionCode code);
}
