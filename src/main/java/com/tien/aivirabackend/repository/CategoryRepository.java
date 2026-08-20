package com.tien.aivirabackend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.tien.aivirabackend.domain.entity.catalog.Category;
import com.tien.aivirabackend.repository.projection.CategoryHighlightProjection;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {
    boolean existsByCategoryNameIgnoreCase(String categoryName);

    boolean existsBySlug(String slug);

    Optional<Category> findBySlug(String slug);

    boolean existsByCategoryNameIgnoreCaseAndIdNot(String categoryName, Long id);

    boolean existsBySlugAndIdNot(String slug, Long id);

    Optional<Category> findBySlugAndActiveTrueAndVisibleTrue(String slug);

    List<Category> findAllByActiveTrueAndVisibleTrue(Sort sort);

    @EntityGraph(attributePaths = "parentCategory")
    Optional<Category> findWithParentCategoryById(Long id);

    @Query("""
            select c.id as categoryId,
            	c.categoryName as categoryName,
            	c.slug as slug,
            	c.description as description,
            	c.imageUrl as imageUrl,
            	c.imagePublicId as imagePublicId,
            	c.displayOrder as displayOrder,
            	count(p.id) as bookCount
            from Category c
            join c.products p
            where c.active = true
            and c.visible = true
            and p.active = true
            and p.status = com.tien.aivirabackend.constant.ProductStatus.ACTIVE
            and p.stockQuantity > 0
            group by c.id, c.categoryName, c.slug, c.description, c.imageUrl, c.imagePublicId, c.displayOrder
            order by count(p.id) desc, c.displayOrder asc, c.categoryName asc
            """)
    List<CategoryHighlightProjection> findCategoryHighlights(Pageable pageable);
}
