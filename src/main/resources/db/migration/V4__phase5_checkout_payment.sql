ALTER TABLE addresses
    ADD COLUMN is_active BIT(1) NOT NULL DEFAULT b'1' AFTER is_default;

CREATE INDEX idx_addresses_user_id ON addresses (user_id);
CREATE INDEX idx_addresses_user_default ON addresses (user_id, is_default);
CREATE INDEX idx_addresses_user_active ON addresses (user_id, is_active);

ALTER TABLE carts
    ADD CONSTRAINT uk_carts_user_id UNIQUE (user_id);

ALTER TABLE cart_items
    ADD CONSTRAINT uk_cart_items_cart_variation UNIQUE (cart_id, product_variation_id);

ALTER TABLE orders
    DROP FOREIGN KEY fk_orders_address;

ALTER TABLE orders
    ADD COLUMN shipping_recipient_name VARCHAR(255) NOT NULL AFTER address_id,
    ADD COLUMN shipping_phone_number VARCHAR(255) NOT NULL AFTER shipping_recipient_name,
    ADD COLUMN shipping_address_line VARCHAR(500) NOT NULL AFTER shipping_phone_number,
    ADD COLUMN shipping_ward VARCHAR(255) AFTER shipping_address_line,
    ADD COLUMN shipping_district VARCHAR(255) AFTER shipping_ward,
    ADD COLUMN shipping_city VARCHAR(255) AFTER shipping_district,
    MODIFY COLUMN address_id BIGINT NULL;

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_address FOREIGN KEY (address_id) REFERENCES addresses (address_id);

CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_status ON orders (order_status);

CREATE TABLE payment_groups (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payment_code VARCHAR(50) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    method VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    provider_txn_ref VARCHAR(100),
    provider_transaction_id VARCHAR(100),
    payment_url VARCHAR(2000),
    deeplink VARCHAR(2000),
    qr_code_url VARCHAR(2000),
    raw_response TEXT,
    expires_at DATETIME(6),
    paid_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payment_groups_payment_code UNIQUE (payment_code),
    CONSTRAINT fk_payment_groups_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE payments
    ADD COLUMN payment_group_id BIGINT NOT NULL AFTER order_id,
    ADD CONSTRAINT fk_payments_payment_group FOREIGN KEY (payment_group_id) REFERENCES payment_groups (id);

CREATE INDEX idx_payment_groups_user_id ON payment_groups (user_id);
CREATE INDEX idx_payment_groups_status_expires ON payment_groups (status, expires_at);
CREATE INDEX idx_payment_groups_provider_ref ON payment_groups (provider_txn_ref);
CREATE INDEX idx_payments_payment_group_id ON payments (payment_group_id);

CREATE TABLE payment_callbacks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    provider VARCHAR(30) NOT NULL,
    event_key VARCHAR(150) NOT NULL,
    payment_code VARCHAR(50),
    status VARCHAR(30) NOT NULL,
    raw_payload TEXT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payment_callbacks_provider_event UNIQUE (provider, event_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_payment_callbacks_payment_code ON payment_callbacks (payment_code);
