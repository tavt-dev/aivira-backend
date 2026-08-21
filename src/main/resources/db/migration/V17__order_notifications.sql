CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recipient_user_id VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    resource_type VARCHAR(50),
    resource_id VARCHAR(100),
    action_url VARCHAR(500),
    payload JSON,
    read_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_notification_recipient_created ON notifications(recipient_user_id, created_at DESC);
CREATE INDEX idx_notification_recipient_read_created ON notifications(recipient_user_id, read_at, created_at DESC);
CREATE INDEX idx_notification_resource ON notifications(resource_type, resource_id);

CREATE TABLE notification_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_key VARCHAR(200) NOT NULL,
    notification_id BIGINT NOT NULL,
    channel VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6) NOT NULL,
    locked_at DATETIME(6),
    processed_at DATETIME(6),
    last_error VARCHAR(1000),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_notification_outbox_event_key UNIQUE (event_key),
    CONSTRAINT fk_notification_outbox_notification FOREIGN KEY (notification_id) REFERENCES notifications(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_notification_outbox_dispatch ON notification_outbox(status, next_attempt_at);
CREATE INDEX idx_notification_outbox_locked ON notification_outbox(status, locked_at);
