package com.tien.aivirabackend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.tien.aivirabackend.domain.entity.blog.BlogPost;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long>, JpaSpecificationExecutor<BlogPost> {
    @Override
    @EntityGraph(attributePaths = { "category", "createdBy" })
    Page<BlogPost> findAll(Specification<BlogPost> specification, Pageable pageable);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsByCategory_IdAndDeletedAtIsNull(Long categoryId);

    @EntityGraph(attributePaths = { "category", "createdBy", "updatedBy", "relatedProducts", "assets" })
    Optional<BlogPost> findDetailedById(Long id);

    @EntityGraph(attributePaths = { "category", "createdBy", "updatedBy", "relatedProducts", "assets" })
    Optional<BlogPost> findDetailedBySlug(String slug);
}
