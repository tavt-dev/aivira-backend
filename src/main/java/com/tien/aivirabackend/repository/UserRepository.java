package com.tien.aivirabackend.repository;

import java.util.Optional;
import java.util.Set;
import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tien.aivirabackend.constant.PermissionCode;
import com.tien.aivirabackend.constant.PredefinedRole;
import com.tien.aivirabackend.domain.entity.user.User;

@Repository
public interface UserRepository extends JpaRepository<User, String>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    @Override
    @EntityGraph(attributePaths = "roles")
    Page<User> findAll(Specification<User> specification, Pageable pageable);

    @EntityGraph(attributePaths = "roles")
    @Query("select u from User u where u.id = :id")
    Optional<User> findWithRolesById(@Param("id") String id);

    @Query(
            """
			select count(distinct u)
			from User u
			join u.roles r
			where r.code = :role
			and u.isActive = true
			and u.isDeleted = false
			""")
    long countActiveUsersByRole(@Param("role") PredefinedRole role);

    @Query("select distinct p.code from User u join u.roles r join r.permissions p where u.id = :userId")
    Set<PermissionCode> findPermissionCodesByUserId(@Param("userId") String userId);

    @Query("select distinct p.code from User u join u.roles r join r.permissions p where u.id = :userId")
    Set<PermissionCode> findRolePermissionCodesByUserId(@Param("userId") String userId);

    long countByIsDeletedFalseAndCreatedAtBetween(Instant fromDate, Instant toDate);
}
