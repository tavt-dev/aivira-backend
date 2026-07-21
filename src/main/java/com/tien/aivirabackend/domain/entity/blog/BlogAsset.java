package com.tien.aivirabackend.domain.entity.blog;

import jakarta.persistence.*;

import com.tien.aivirabackend.domain.entity.BaseEntity;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "blog_assets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlogAsset extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 1000)
    String url;

    @Column(name = "public_id", nullable = false, unique = true, length = 255)
    String publicId;

    @Column(name = "alt_text", length = 255)
    String altText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    BlogPost post;
}
