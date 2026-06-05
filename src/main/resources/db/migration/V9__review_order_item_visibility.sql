ALTER TABLE reviews
    ADD COLUMN order_item_id BIGINT NULL AFTER order_id,
    ADD COLUMN is_visible BIT(1) NOT NULL DEFAULT b'1' AFTER is_approved,
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER is_visible,
    ADD COLUMN moderated_by VARCHAR(255) NULL AFTER admin_reply,
    ADD COLUMN moderated_at DATETIME(6) NULL AFTER moderated_by,
    ADD COLUMN replied_by VARCHAR(255) NULL AFTER moderated_at,
    ADD COLUMN replied_at DATETIME(6) NULL AFTER replied_by;

ALTER TABLE reviews
    ADD CONSTRAINT fk_reviews_order_item FOREIGN KEY (order_item_id) REFERENCES order_items (id),
    ADD CONSTRAINT uk_reviews_order_item UNIQUE (order_item_id);

CREATE INDEX idx_reviews_product ON reviews (product_id);
CREATE INDEX idx_reviews_user ON reviews (user_id);
CREATE INDEX idx_reviews_approved ON reviews (is_approved);
CREATE INDEX idx_reviews_visible ON reviews (is_visible);
CREATE INDEX idx_reviews_created_at ON reviews (created_at);
