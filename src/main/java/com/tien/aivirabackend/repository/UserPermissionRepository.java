package com.tien.aivirabackend.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tien.aivirabackend.constant.PermissionCode;
import com.tien.aivirabackend.domain.entity.user.UserPermission;

@Repository
public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {
    @Query(
            """
			select up.permission.code
			from UserPermission up
			where up.user.id = :userId
			and up.active = true
			and up.revokedAt is null
			and (up.expiresAt is null or up.expiresAt > :now)
			""")
    Set<PermissionCode> findActivePermissionCodesByUserId(@Param("userId") String userId, @Param("now") Instant now);

    @EntityGraph(attributePaths = {"permission", "grantedBy"})
    @Query(
            """
			select up
			from UserPermission up
			where up.user.id = :userId
			order by up.active desc, up.grantedAt desc
			""")
    List<UserPermission> findAllByUserId(@Param("userId") String userId);

    @EntityGraph(attributePaths = {"permission", "grantedBy"})
    Optional<UserPermission> findFirstByUser_IdAndPermission_CodeAndActiveTrueOrderByGrantedAtDesc(
            String userId, PermissionCode permissionCode);

    boolean existsByUser_IdAndPermission_CodeAndActiveTrue(String userId, PermissionCode permissionCode);
}
