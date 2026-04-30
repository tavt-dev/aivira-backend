package com.tien.aivirabackend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.tien.aivirabackend.domain.entity.catalog.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    boolean existsBySku(String sku);

    boolean existsBySlug(String slug);

    boolean existsBySkuAndIdNot(String sku, Long id);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @EntityGraph(attributePaths = {"shop", "shop.owner", "category", "productVariations", "productMedia"})
    Optional<Product> findDetailedById(Long id);

    @EntityGraph(attributePaths = {"shop", "category", "productVariations", "productMedia"})
    Optional<Product> findDetailedBySlug(String slug);
}
