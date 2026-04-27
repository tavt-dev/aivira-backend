package com.tien.aivirabackend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.tien.aivirabackend.constant.ShopStatus;
import com.tien.aivirabackend.domain.entity.marketplace.Shop;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long>, JpaSpecificationExecutor<Shop> {
    boolean existsByOwnerId(String ownerId);

    boolean existsBySlug(String slug);

    Optional<Shop> findByOwnerId(String ownerId);

    @EntityGraph(attributePaths = "owner")
    Optional<Shop> findWithOwnerById(Long id);

    @EntityGraph(attributePaths = "owner")
    Optional<Shop> findWithOwnerByOwnerId(String ownerId);

    @EntityGraph(attributePaths = "owner")
    Page<Shop> findAllByStatus(ShopStatus status, Pageable pageable);
}
