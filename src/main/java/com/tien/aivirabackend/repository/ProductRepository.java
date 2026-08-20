package com.tien.aivirabackend.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tien.aivirabackend.constant.ProductStatus;
import com.tien.aivirabackend.domain.entity.catalog.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    @Modifying(flushAutomatically = true)
    @Query("update Product p set p.soldCount = coalesce(p.soldCount, 0) + :quantity where p.id = :productId")
    int incrementSoldCount(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    boolean existsBySku(String sku);

    boolean existsBySlug(String slug);

    Optional<Product> findBySku(String sku);

    Optional<Product> findBySlug(String slug);

    boolean existsBySkuAndIdNot(String sku, Long id);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdNot(String isbn, Long id);

    @EntityGraph(attributePaths = { "category", "productVariations", "productMedia" })
    Optional<Product> findDetailedById(Long id);

    @EntityGraph(attributePaths = { "category", "productVariations", "productMedia" })
    Optional<Product> findDetailedBySlug(String slug);

    Page<Product> findByActiveTrueAndStatusAndStockQuantityLessThanEqual(ProductStatus status, Integer stockQuantity,
            Pageable pageable);

    long countByActiveTrueAndStatusAndStockQuantityLessThanEqual(ProductStatus status, Integer stockQuantity);

    Page<Product> findByActiveTrueAndStatusOrderBySoldCountDescCreatedAtDesc(ProductStatus status, Pageable pageable);

    @EntityGraph(attributePaths = { "category" })
    List<Product> findByActiveTrueAndStatusAndStockQuantityGreaterThan(ProductStatus status, Integer stockQuantity);
}
