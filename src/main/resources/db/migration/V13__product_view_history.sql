CREATE TABLE product_view_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    user_id VARCHAR(255),
    anonymous_id_hash CHAR(64),
    session_id_hash CHAR(64),
    viewer_key CHAR(64) NOT NULL,
    source VARCHAR(30) NOT NULL,
    referrer_path VARCHAR(500),
    viewed_at DATETIME(6) NOT NULL,
    deduplication_bucket BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_view_events_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT uk_product_view_event_dedup UNIQUE (viewer_key, product_id, deduplication_bucket),
    INDEX idx_product_view_events_user_time (user_id, viewed_at),
    INDEX idx_product_view_events_anonymous_time (anonymous_id_hash, viewed_at),
    INDEX idx_product_view_events_product_time (product_id, viewed_at),
    INDEX idx_product_view_events_source_time (source, viewed_at),
    INDEX idx_product_view_events_viewed_at (viewed_at)
);

CREATE TABLE user_recently_viewed (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(255) NOT NULL,
    product_id BIGINT NOT NULL,
    first_viewed_at DATETIME(6) NOT NULL,
    last_viewed_at DATETIME(6) NOT NULL,
    view_count BIGINT NOT NULL DEFAULT 1,
    last_source VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_recently_viewed_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT uk_user_recently_viewed_user_product UNIQUE (user_id, product_id),
    INDEX idx_user_recently_viewed_user_time (user_id, last_viewed_at)
);

SELECT CHARACTER_SET_NAME, COLLATION_NAME
INTO @view_user_charset, @view_user_collation
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'id';

SET @align_view_user_columns = CONCAT(
    'ALTER TABLE product_view_events MODIFY user_id VARCHAR(255) CHARACTER SET ',
    @view_user_charset, ' COLLATE ', @view_user_collation, ' NULL, ',
    'ADD CONSTRAINT fk_product_view_events_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL'
);
PREPARE align_view_event_user_statement FROM @align_view_user_columns;
EXECUTE align_view_event_user_statement;
DEALLOCATE PREPARE align_view_event_user_statement;

SET @align_recent_user_column = CONCAT(
    'ALTER TABLE user_recently_viewed MODIFY user_id VARCHAR(255) CHARACTER SET ',
    @view_user_charset, ' COLLATE ', @view_user_collation, ' NOT NULL, ',
    'ADD CONSTRAINT fk_user_recently_viewed_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE'
);
PREPARE align_recent_user_statement FROM @align_recent_user_column;
EXECUTE align_recent_user_statement;
DEALLOCATE PREPARE align_recent_user_statement;
