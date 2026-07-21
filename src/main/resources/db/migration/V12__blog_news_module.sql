CREATE TABLE blog_categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    display_order INT NOT NULL DEFAULT 0,
    is_active BIT(1) NOT NULL DEFAULT b'1',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_blog_categories_slug UNIQUE (slug)
);

CREATE TABLE blog_posts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    excerpt VARCHAR(500) NOT NULL,
    content_html LONGTEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    cover_url VARCHAR(1000),
    cover_public_id VARCHAR(255),
    cover_alt_text VARCHAR(255),
    seo_title VARCHAR(70),
    meta_description VARCHAR(160),
    category_id BIGINT NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    updated_by VARCHAR(255) NOT NULL,
    published_at DATETIME(6),
    deleted_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_blog_posts_slug UNIQUE (slug),
    CONSTRAINT fk_blog_posts_category FOREIGN KEY (category_id) REFERENCES blog_categories (id),
    INDEX idx_blog_posts_status (status),
    INDEX idx_blog_posts_published_at (published_at),
    INDEX idx_blog_posts_category (category_id),
    INDEX idx_blog_posts_deleted_at (deleted_at)
);

-- String columns participating in a MySQL foreign key must use the exact same
-- character set and collation. Older databases may have been created with a
-- different default collation, so inherit these attributes from users.id.
SELECT CHARACTER_SET_NAME, COLLATION_NAME
INTO @users_id_charset, @users_id_collation
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'users'
  AND COLUMN_NAME = 'id';

SET @align_blog_author_columns = CONCAT(
    'ALTER TABLE blog_posts ',
    'MODIFY created_by VARCHAR(255) CHARACTER SET ', @users_id_charset,
    ' COLLATE ', @users_id_collation, ' NOT NULL, ',
    'MODIFY updated_by VARCHAR(255) CHARACTER SET ', @users_id_charset,
    ' COLLATE ', @users_id_collation, ' NOT NULL'
);
PREPARE align_blog_author_columns_statement FROM @align_blog_author_columns;
EXECUTE align_blog_author_columns_statement;
DEALLOCATE PREPARE align_blog_author_columns_statement;

ALTER TABLE blog_posts
    ADD CONSTRAINT fk_blog_posts_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    ADD CONSTRAINT fk_blog_posts_updated_by FOREIGN KEY (updated_by) REFERENCES users (id);

CREATE TABLE blog_post_products (
    post_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    PRIMARY KEY (post_id, product_id),
    CONSTRAINT fk_blog_post_products_post FOREIGN KEY (post_id) REFERENCES blog_posts (id),
    CONSTRAINT fk_blog_post_products_product FOREIGN KEY (product_id) REFERENCES products (id),
    INDEX idx_blog_post_products_product (product_id)
);

CREATE TABLE blog_assets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    url VARCHAR(1000) NOT NULL,
    public_id VARCHAR(255) NOT NULL,
    alt_text VARCHAR(255),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_blog_assets_public_id UNIQUE (public_id),
    CONSTRAINT fk_blog_assets_post FOREIGN KEY (post_id) REFERENCES blog_posts (id),
    INDEX idx_blog_assets_post (post_id)
);
