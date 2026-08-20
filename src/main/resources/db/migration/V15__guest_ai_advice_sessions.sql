ALTER TABLE ai_advice_sessions
    MODIFY COLUMN user_id VARCHAR(255) NULL;

ALTER TABLE ai_advice_sessions
    ADD COLUMN guest_key VARCHAR(36);

CREATE INDEX idx_ai_advice_sessions_guest_key
    ON ai_advice_sessions (guest_key);

ALTER TABLE ai_advice_sessions
    ADD CONSTRAINT ck_ai_advice_session_owner
    CHECK ((user_id IS NOT NULL AND guest_key IS NULL)
        OR (user_id IS NULL AND guest_key IS NOT NULL));
