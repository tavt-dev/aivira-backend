ALTER TABLE ai_advice_messages
    ADD COLUMN provider VARCHAR(30) NULL AFTER response_status;
