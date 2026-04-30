ALTER TABLE products
    ADD COLUMN shop_id BIGINT NOT NULL AFTER id,
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' AFTER is_featured,
    ADD COLUMN rejection_reason VARCHAR(500) AFTER status,
    ADD COLUMN submitted_at DATETIME(6) AFTER rejection_reason,
    ADD COLUMN approved_by VARCHAR(255) AFTER submitted_at,
    ADD COLUMN approved_at DATETIME(6) AFTER approved_by,
    ADD COLUMN rejected_by VARCHAR(255) AFTER approved_at,
    ADD COLUMN rejected_at DATETIME(6) AFTER rejected_by;

ALTER TABLE products
    ADD CONSTRAINT fk_products_shop FOREIGN KEY (shop_id) REFERENCES shops (id),
    ADD CONSTRAINT fk_products_approved_by FOREIGN KEY (approved_by) REFERENCES users (id),
    ADD CONSTRAINT fk_products_rejected_by FOREIGN KEY (rejected_by) REFERENCES users (id);

CREATE INDEX idx_products_shop_id ON products (shop_id);
CREATE INDEX idx_products_status ON products (status);
CREATE INDEX idx_products_category_id ON products (category_id);
CREATE INDEX idx_products_brand ON products (brand);
CREATE INDEX idx_products_created_at ON products (created_at);
