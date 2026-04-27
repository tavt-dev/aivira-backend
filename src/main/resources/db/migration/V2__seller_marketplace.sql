CREATE TABLE shops (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id VARCHAR(255) NOT NULL,
    shop_name VARCHAR(150) NOT NULL,
    slug VARCHAR(180) NOT NULL,
    logo_url VARCHAR(255),
    logo_public_id VARCHAR(255),
    description VARCHAR(1000),
    business_email VARCHAR(120) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    legal_name VARCHAR(150) NOT NULL,
    tax_code VARCHAR(50),
    pickup_address_line VARCHAR(500) NOT NULL,
    pickup_ward VARCHAR(120),
    pickup_district VARCHAR(120),
    pickup_city VARCHAR(120) NOT NULL,
    status VARCHAR(30) NOT NULL,
    rejection_reason VARCHAR(500),
    locked_reason VARCHAR(500),
    approved_by VARCHAR(255),
    approved_at DATETIME(6),
    rejected_by VARCHAR(255),
    rejected_at DATETIME(6),
    locked_by VARCHAR(255),
    locked_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_shops_owner UNIQUE (owner_id),
    CONSTRAINT uk_shops_slug UNIQUE (slug),
    CONSTRAINT fk_shops_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT fk_shops_approved_by FOREIGN KEY (approved_by) REFERENCES users (id),
    CONSTRAINT fk_shops_rejected_by FOREIGN KEY (rejected_by) REFERENCES users (id),
    CONSTRAINT fk_shops_locked_by FOREIGN KEY (locked_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_shops_status ON shops (status);
CREATE INDEX idx_shops_shop_name ON shops (shop_name);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'SELLER_APPLY'
WHERE r.code = 'USER'
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions existing
      WHERE existing.role_id = r.id
        AND existing.permission_id = p.id
  );
