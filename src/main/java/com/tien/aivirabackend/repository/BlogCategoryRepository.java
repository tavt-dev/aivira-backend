package com.tien.aivirabackend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tien.aivirabackend.domain.entity.blog.BlogCategory;

public interface BlogCategoryRepository extends JpaRepository<BlogCategory, Long> {
    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    List<BlogCategory> findByActiveTrueOrderByDisplayOrderAscNameAsc();

    List<BlogCategory> findAllByOrderByDisplayOrderAscNameAsc();
}
