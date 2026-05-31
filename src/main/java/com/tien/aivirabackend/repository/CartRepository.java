package com.tien.aivirabackend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tien.aivirabackend.domain.entity.transaction.Cart;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    @EntityGraph(
            attributePaths = {
                "items",
                "items.productVariation",
                "items.productVariation.product",
                "items.productVariation.product.category"
            })
    Optional<Cart> findByUserIdAndActiveTrue(String userId);
}
