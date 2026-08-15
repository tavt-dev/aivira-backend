CREATE TABLE ai_advice_sessions (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    locale VARCHAR(10) NOT NULL,
    personalization_enabled BIT(1) NOT NULL,
    last_activity_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ai_advice_sessions_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_ai_advice_sessions_user_activity (user_id, last_activity_at),
    INDEX idx_ai_advice_sessions_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ai_advice_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_id VARCHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL,
    content LONGTEXT NOT NULL,
    client_message_id VARCHAR(36),
    response_status VARCHAR(30),
    model VARCHAR(100),
    input_tokens INT,
    output_tokens INT,
    latency_ms BIGINT,
    error_code VARCHAR(80),
    suggested_prompts TEXT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ai_advice_messages_session FOREIGN KEY (session_id) REFERENCES ai_advice_sessions (id) ON DELETE CASCADE,
    CONSTRAINT uk_ai_advice_client_message UNIQUE (session_id, client_message_id),
    INDEX idx_ai_advice_messages_session_created (session_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ai_advice_result_snapshots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    message_id BIGINT NOT NULL,
    search_profile LONGTEXT NOT NULL,
    total_results INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_advice_snapshot_message UNIQUE (message_id),
    CONSTRAINT fk_ai_advice_snapshot_message FOREIGN KEY (message_id) REFERENCES ai_advice_messages (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ai_advice_recommendations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    snapshot_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    rank_position INT NOT NULL,
    reason TEXT,
    matched_criteria TEXT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_advice_snapshot_rank UNIQUE (snapshot_id, rank_position),
    CONSTRAINT uk_ai_advice_snapshot_product UNIQUE (snapshot_id, product_id),
    CONSTRAINT fk_ai_advice_recommendation_snapshot FOREIGN KEY (snapshot_id) REFERENCES ai_advice_result_snapshots (id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_advice_recommendation_product FOREIGN KEY (product_id) REFERENCES products (id),
    INDEX idx_ai_advice_recommendation_page (snapshot_id, rank_position)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ai_advice_monthly_quotas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(255) NOT NULL,
    period_key VARCHAR(7) NOT NULL,
    used_count INT NOT NULL,
    reserved_count INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_advice_quota_period UNIQUE (user_id, period_key),
    CONSTRAINT fk_ai_advice_quota_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ai_advice_usages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(255) NOT NULL,
    period_key VARCHAR(7) NOT NULL,
    client_message_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_advice_usage_request UNIQUE (user_id, period_key, client_message_id),
    CONSTRAINT fk_ai_advice_usage_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_ai_advice_usage_status (user_id, period_key, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ai_advice_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_id VARCHAR(36) NOT NULL,
    message_id BIGINT,
    recommendation_id BIGINT,
    event_type VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ai_advice_event_session FOREIGN KEY (session_id) REFERENCES ai_advice_sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_advice_event_message FOREIGN KEY (message_id) REFERENCES ai_advice_messages (id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_advice_event_recommendation FOREIGN KEY (recommendation_id) REFERENCES ai_advice_recommendations (id) ON DELETE CASCADE,
    INDEX idx_ai_advice_event_session_type (session_id, event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
