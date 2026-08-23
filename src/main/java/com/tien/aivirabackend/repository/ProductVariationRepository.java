package com.tien.aivirabackend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.tien.aivirabackend.domain.entity.catalog.ProductVariation;

@Repository
public interface ProductVariationRepository
        extends JpaRepository<ProductVariation, Long>, JpaSpecificationExecutor<ProductVariation> {
    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    Optional<ProductVariation> findByIdAndProductId(Long id, Long productId);

    List<ProductVariation> findByProductIdInOrderByProductIdAscIdAsc(Collection<Long> productIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from ProductVariation v join fetch v.product p where v.id in :ids order by v.id asc")
    List<ProductVariation> findAllByIdInForUpdate(Collection<Long> ids);
}
