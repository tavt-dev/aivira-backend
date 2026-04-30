package com.tien.aivirabackend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.tien.aivirabackend.domain.entity.catalog.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {
    boolean existsByCategoryNameIgnoreCase(String categoryName);

    boolean existsBySlug(String slug);

    boolean existsByCategoryNameIgnoreCaseAndIdNot(String categoryName, Long id);

    boolean existsBySlugAndIdNot(String slug, Long id);

    Optional<Category> findBySlugAndActiveTrueAndVisibleTrue(String slug);

    List<Category> findAllByActiveTrueAndVisibleTrue(Sort sort);

    @EntityGraph(attributePaths = "parentCategory")
    Optional<Category> findWithParentCategoryById(Long id);
}
