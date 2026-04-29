package com.tien.aivirabackend.repository;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tien.aivirabackend.constant.PermissionCode;
import com.tien.aivirabackend.domain.entity.user.User;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    @Query("select distinct p.code from User u join u.roles r join r.permissions p where u.id = :userId")
    Set<PermissionCode> findPermissionCodesByUserId(@Param("userId") String userId);

    @Query("select distinct p.code from User u join u.roles r join r.permissions p where u.id = :userId")
    Set<PermissionCode> findRolePermissionCodesByUserId(@Param("userId") String userId);
}
