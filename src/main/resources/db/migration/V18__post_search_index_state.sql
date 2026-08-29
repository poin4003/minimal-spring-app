-- Durable state for bounded post search indexing and recovery.

CREATE DOMAIN PostSearchIndexStatusEnum AS VARCHAR(16)
    CHECK (VALUE IN (
        'PENDING',
        'QUEUED',
        'PROCESSING',
        'SYNCED',
        'FAILED'
    ));

CREATE TABLE post_search_index_state (
    post_id UUID PRIMARY KEY,
    status PostSearchIndexStatusEnum NOT NULL,
    requested_revision BIGINT DEFAULT 1 NOT NULL,
    processed_revision BIGINT DEFAULT 0 NOT NULL,
    indexed_source_updated_at TIMESTAMP,
    indexed_model_version VARCHAR(255),
    indexed_generation UUID,
    attempt_count INTEGER DEFAULT 0 NOT NULL,
    next_attempt_at TIMESTAMP,
    lease_token UUID,
    lease_expires_at TIMESTAMP,
    last_error VARCHAR(2000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_post_search_state_revisions CHECK (
        requested_revision > 0
        AND processed_revision >= 0
        AND processed_revision <= requested_revision
    ),
    CONSTRAINT ck_post_search_state_attempt_count CHECK (
        attempt_count >= 0
    )
);

CREATE INDEX idx_post_search_state_status_retry
    ON post_search_index_state(status, next_attempt_at, updated_at);

CREATE INDEX idx_post_search_state_status_lease
    ON post_search_index_state(status, lease_expires_at, updated_at);

CREATE INDEX idx_post_search_state_status_generation
    ON post_search_index_state(status, indexed_generation, updated_at);
