CREATE TABLE refunds (
    id BIGINT NOT NULL AUTO_INCREMENT,
    refund_code VARCHAR(50) NOT NULL,
    order_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    note VARCHAR(1000) NOT NULL,
    status VARCHAR(30) NOT NULL,
    refunded_by VARCHAR(255) NOT NULL,
    refunded_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refunds_refund_code UNIQUE (refund_code),
    CONSTRAINT uk_refunds_order UNIQUE (order_id),
    CONSTRAINT fk_refunds_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
