package com.tien.aivirabackend.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.tien.aivirabackend.domain.entity.review.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {
    boolean existsByOrderItem_Id(Long orderItemId);

    @EntityGraph(attributePaths = {"images", "user", "product", "productVariation", "order", "orderItem"})
    Optional<Review> findDetailedById(Long id);

    @EntityGraph(attributePaths = {"images", "user", "product", "productVariation", "order", "orderItem"})
    Optional<Review> findDetailedByIdAndUserId(Long id, String userId);

    @EntityGraph(attributePaths = {"product", "product.category"})
    List<Review> findTop20ByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(String userId);
}
