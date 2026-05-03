CREATE TABLE payment_attempts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payment_group_id BIGINT NOT NULL,
    provider VARCHAR(30) NOT NULL,
    method VARCHAR(30) NOT NULL,
    attempt_no INT NOT NULL,
    provider_txn_ref VARCHAR(100),
    request_id VARCHAR(100),
    status VARCHAR(30) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    payment_url VARCHAR(2000),
    deeplink VARCHAR(2000),
    qr_code_url VARCHAR(2000),
    provider_transaction_id VARCHAR(100),
    raw_request TEXT,
    raw_response TEXT,
    expires_at DATETIME(6),
    completed_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_payment_attempts_group FOREIGN KEY (payment_group_id) REFERENCES payment_groups (id),
    CONSTRAINT uk_payment_attempts_group_attempt UNIQUE (payment_group_id, attempt_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_payment_attempts_provider_ref ON payment_attempts (provider, provider_txn_ref);
CREATE INDEX idx_payment_attempts_request_id ON payment_attempts (request_id);
CREATE INDEX idx_payment_attempts_group_status ON payment_attempts (payment_group_id, status);

INSERT INTO permissions (code, name, description, permission_group, is_system, created_at, updated_at)
SELECT 'PAYMENT_RECONCILE',
       'Payment reconcile',
       'System permission PAYMENT_RECONCILE',
       'PAYMENT',
       b'1',
       CURRENT_TIMESTAMP(6),
       CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'PAYMENT_RECONCILE'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'PAYMENT_RECONCILE'
WHERE r.code = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
