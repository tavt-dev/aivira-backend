package com.tien.aivirabackend.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tien.aivirabackend.domain.entity.analytics.UserRecentlyViewed;

public interface UserRecentlyViewedRepository extends JpaRepository<UserRecentlyViewed, Long> {
    Optional<UserRecentlyViewed> findByUserIdAndProductId(String userId, Long productId);

    @EntityGraph(attributePaths = {"product", "product.category", "product.productVariations", "product.productMedia"})
    @Query(
            """
			select r from UserRecentlyViewed r
			where r.user.id = :userId
			and r.product.status = com.tien.aivirabackend.constant.ProductStatus.ACTIVE
			and r.product.active = true
			and r.product.category.active = true
			and r.product.category.visible = true
			""")
    Page<UserRecentlyViewed> findVisibleByUserId(@Param("userId") String userId, Pageable pageable);

    long deleteByUserIdAndProductId(String userId, Long productId);

    long deleteByUserId(String userId);

    @Modifying
    long deleteByLastViewedAtBefore(Instant cutoff);

    @Modifying
    @Query(
            value =
                    """
			DELETE FROM user_recently_viewed
			WHERE id IN (
				SELECT id FROM (
					SELECT id, ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY last_viewed_at DESC) AS row_num
					FROM user_recently_viewed
				) ranked WHERE row_num > :maximum
			)
			""",
            nativeQuery = true)
    int trimToMaximumPerUser(@Param("maximum") int maximum);
}
