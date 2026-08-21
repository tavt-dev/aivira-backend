CREATE TABLE rag_product_index (
    product_id BIGINT NOT NULL,
    content_hash VARCHAR(64),
    provider VARCHAR(30),
    model VARCHAR(100),
    dimension INT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    last_error VARCHAR(1000),
    indexed_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (product_id),
    CONSTRAINT fk_rag_product_index_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    INDEX idx_rag_product_index_status (status, updated_at)
);

CREATE TABLE rag_index_jobs (
    id VARCHAR(36) NOT NULL,
    job_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_items INT NOT NULL DEFAULT 0,
    succeeded_items INT NOT NULL DEFAULT 0,
    failed_items INT NOT NULL DEFAULT 0,
    error_message VARCHAR(1000),
    started_at TIMESTAMP(6),
    completed_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_rag_index_jobs_status (status, created_at)
);

ALTER TABLE ai_advice_messages
    ADD COLUMN retrieval_mode VARCHAR(30) NULL AFTER provider,
    ADD COLUMN embedding_provider VARCHAR(30) NULL AFTER retrieval_mode,
    ADD COLUMN embedding_model VARCHAR(100) NULL AFTER embedding_provider;

ALTER TABLE ai_advice_recommendations
    ADD COLUMN semantic_score DOUBLE NULL,
    ADD COLUMN lexical_score DOUBLE NULL,
    ADD COLUMN final_score DOUBLE NULL;
