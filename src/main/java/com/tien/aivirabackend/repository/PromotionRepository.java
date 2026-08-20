package com.tien.aivirabackend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tien.aivirabackend.domain.entity.discount.Promotion;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    boolean existsByPromotionName(String promotionName);

    boolean existsByPromotionNameAndIdNot(String promotionName, Long id);

    @Query("""
            select p
            from Promotion p
            where p.active = true
            and p.startAt <= :now
            and p.endAt >= :now
            """)
    List<Promotion> findActiveAt(@Param("now") LocalDateTime now);
}
