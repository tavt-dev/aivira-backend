package com.tien.aivirabackend.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tien.aivirabackend.domain.entity.analytics.ProductViewEvent;

public interface ProductViewEventRepository extends JpaRepository<ProductViewEvent, Long> {
    @Modifying
    @Query(
            value =
                    """
			INSERT IGNORE INTO product_view_events
				(product_id, user_id, anonymous_id_hash, session_id_hash, viewer_key, source,
				referrer_path, viewed_at, deduplication_bucket, created_at)
			VALUES (:productId, :userId, :anonymousHash, :sessionHash, :viewerKey, :source,
					:referrerPath, :viewedAt, :bucket, :viewedAt)
			""",
            nativeQuery = true)
    int insertIfAbsent(
            @Param("productId") Long productId,
            @Param("userId") String userId,
            @Param("anonymousHash") String anonymousHash,
            @Param("sessionHash") String sessionHash,
            @Param("viewerKey") String viewerKey,
            @Param("source") String source,
            @Param("referrerPath") String referrerPath,
            @Param("viewedAt") Instant viewedAt,
            @Param("bucket") long bucket);

    List<ProductViewEvent> findByAnonymousIdHashAndUserIsNullAndViewedAtGreaterThanEqual(
            String anonymousIdHash, Instant cutoff);

    @Modifying
    @Query("update ProductViewEvent e set e.anonymousIdHash = e.viewerKey, e.user = null where e.user.id = :userId")
    int anonymizeByUserId(@Param("userId") String userId);

    @Modifying
    long deleteByUserIsNullAndViewedAtBefore(Instant cutoff);

    @Modifying
    long deleteByViewedAtBefore(Instant cutoff);
}
