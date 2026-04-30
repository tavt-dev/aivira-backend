package com.tien.aivirabackend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.tien.aivirabackend.domain.entity.catalog.ProductMedia;

@Repository
public interface ProductMediaRepository
        extends JpaRepository<ProductMedia, Long>, JpaSpecificationExecutor<ProductMedia> {
    Optional<ProductMedia> findByIdAndProductId(Long id, Long productId);
}
