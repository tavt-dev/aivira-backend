ALTER TABLE coupon_usages
    MODIFY used_at DATETIME(6) NULL,
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'FINALIZED' AFTER used_at,
    ADD COLUMN reserved_at DATETIME(6) NULL AFTER status,
    ADD COLUMN finalized_at DATETIME(6) NULL AFTER reserved_at,
    ADD COLUMN released_at DATETIME(6) NULL AFTER finalized_at;

UPDATE coupon_usages
SET finalized_at = used_at
WHERE status = 'FINALIZED'
  AND used_at IS NOT NULL;

ALTER TABLE coupon_usages
    ADD CONSTRAINT uk_coupon_usages_coupon_order UNIQUE (coupon_id, order_id);
