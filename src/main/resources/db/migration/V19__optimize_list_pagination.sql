CREATE INDEX idx_products_active_status_created_id
    ON products (is_active, status, created_at DESC, id DESC);

CREATE INDEX idx_blog_posts_status_deleted_published_id
    ON blog_posts (status, deleted_at, published_at DESC, id DESC);

CREATE INDEX idx_reviews_product_visibility_created_id
    ON reviews (product_id, is_approved, is_visible, deleted_at, created_at DESC, id DESC);

CREATE INDEX idx_users_deleted_created_id
    ON users (is_deleted, created_at DESC, id);

CREATE INDEX idx_notifications_recipient_created_id
    ON notifications (recipient_user_id, created_at DESC, id DESC);

CREATE INDEX idx_notifications_recipient_read_created_id
    ON notifications (recipient_user_id, read_at, created_at DESC, id DESC);
