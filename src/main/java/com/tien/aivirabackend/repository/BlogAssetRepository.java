package com.tien.aivirabackend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tien.aivirabackend.domain.entity.blog.BlogAsset;

public interface BlogAssetRepository extends JpaRepository<BlogAsset, Long> {
    Optional<BlogAsset> findByIdAndPost_Id(Long id, Long postId);
}
