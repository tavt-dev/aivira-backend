package com.tien.aivirabackend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tien.aivirabackend.domain.entity.transaction.CartItem;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartIdAndProductVariationId(Long cartId, Long productVariationId);

    Optional<CartItem> findByIdAndCartUserIdAndCartActiveTrue(Long id, String userId);

    List<CartItem> findByIdInAndCartUserIdAndCartActiveTrue(Collection<Long> ids, String userId);

    void deleteByCartId(Long cartId);

    @Modifying
    @Query(
            "delete from CartItem ci where ci.cart.user.id = :userId and ci.cart.active = true and ci.productVariation.id in :variationIds")
    void deleteActiveCartItemsByUserIdAndVariationIds(
            @Param("userId") String userId, @Param("variationIds") Collection<Long> variationIds);
}
