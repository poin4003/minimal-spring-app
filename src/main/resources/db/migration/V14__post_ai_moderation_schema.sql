-- Schema frame for AI-assisted post moderation.

CREATE DOMAIN PostAiModerationModeEnum AS VARCHAR(16)
    CHECK (VALUE IN ('MANUAL', 'AUTO'));

CREATE DOMAIN PostAiModerationOutcomeEnum AS VARCHAR(16)
    CHECK (VALUE IN ('APPROVE', 'REJECT', 'ESCALATE', 'ERROR'));

CREATE TABLE post_ai_moderation_config (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    mode PostAiModerationModeEnum NOT NULL,
    prompt_text TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_post_ai_moderation_config_code
    ON post_ai_moderation_config(code);

INSERT INTO post_ai_moderation_config (
    id,
    code,
    mode,
    prompt_text
) VALUES (
    '8a77f380-1e8f-4c84-93d2-2607a4dbb001',
    'DEFAULT',
    'MANUAL',
    NULL
);

CREATE TABLE post_ai_moderation_decision_log (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL,
    outcome PostAiModerationOutcomeEnum NOT NULL,
    prompt_snapshot TEXT NOT NULL,
    reason TEXT,
    raw_response TEXT,
    error_message TEXT,
    model_name VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_post_ai_moderation_log_post
        FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE
);

CREATE INDEX idx_post_ai_moderation_log_post_created_at
    ON post_ai_moderation_decision_log(post_id, created_at);
