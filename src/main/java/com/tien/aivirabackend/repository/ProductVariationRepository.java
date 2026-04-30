package com.tien.aivirabackend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.tien.aivirabackend.domain.entity.catalog.ProductVariation;

@Repository
public interface ProductVariationRepository
        extends JpaRepository<ProductVariation, Long>, JpaSpecificationExecutor<ProductVariation> {
    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    Optional<ProductVariation> findByIdAndProductId(Long id, Long productId);
}
