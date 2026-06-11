CREATE TABLE oauth_login_states (
    id BIGINT NOT NULL AUTO_INCREMENT,
    state_hash VARCHAR(64) NOT NULL,
    next_path VARCHAR(500),
    device_info VARCHAR(512),
    ip_address VARCHAR(45),
    expires_at DATETIME(6) NOT NULL,
    consumed_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_oauth_login_states_state_hash UNIQUE (state_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE oauth_login_tickets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ticket_hash VARCHAR(64) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    device_info VARCHAR(512),
    ip_address VARCHAR(45),
    expires_at DATETIME(6) NOT NULL,
    consumed_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_oauth_login_tickets_ticket_hash UNIQUE (ticket_hash),
    CONSTRAINT fk_oauth_login_tickets_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE UNIQUE INDEX uk_users_provider_user_id ON users (provider, provider_user_id);
CREATE INDEX idx_oauth_login_states_expires_at ON oauth_login_states (expires_at);
CREATE INDEX idx_oauth_login_tickets_expires_at ON oauth_login_tickets (expires_at);
