-- Separate the owner-controlled post lifecycle from moderation workflow.

CREATE DOMAIN PostLifecycleStatusEnum AS VARCHAR(16)
    CHECK (VALUE IN ('DRAFT', 'ACTIVE', 'ARCHIVED', 'DELETED'));

ALTER TABLE post
    ADD COLUMN lifecycle_status PostLifecycleStatusEnum
        DEFAULT 'ACTIVE' NOT NULL;

ALTER TABLE post
    ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE post
    ALTER COLUMN moderation_status DROP NOT NULL;

ALTER TABLE post
    DROP CONSTRAINT ck_post_moderation_state;

ALTER TABLE post
    ADD CONSTRAINT ck_post_lifecycle_state CHECK (
        (
            lifecycle_status = 'DRAFT'
            AND moderation_status IS NULL
            AND deleted_at IS NULL
        )
        OR (
            lifecycle_status IN ('ACTIVE', 'ARCHIVED')
            AND moderation_status IS NOT NULL
            AND deleted_at IS NULL
        )
        OR (
            lifecycle_status = 'DELETED'
            AND deleted_at IS NOT NULL
        )
    );

ALTER TABLE post
    ADD CONSTRAINT ck_post_moderation_state CHECK (
        (
            moderation_status IS NULL
            AND published_at IS NULL
            AND moderated_by_id IS NULL
            AND moderated_at IS NULL
            AND rejection_reason IS NULL
        )
        OR (
            moderation_status = 'PENDING_REVIEW'
            AND published_at IS NULL
            AND moderated_by_id IS NULL
            AND moderated_at IS NULL
            AND rejection_reason IS NULL
        )
        OR (
            moderation_status = 'PUBLISHED'
            AND published_at IS NOT NULL
            AND moderated_by_id IS NOT NULL
            AND moderated_at IS NOT NULL
            AND rejection_reason IS NULL
        )
        OR (
            moderation_status = 'REJECTED'
            AND published_at IS NULL
            AND moderated_by_id IS NOT NULL
            AND moderated_at IS NOT NULL
            AND rejection_reason IS NOT NULL
            AND CHAR_LENGTH(TRIM(rejection_reason)) > 0
        )
    );

DROP INDEX idx_post_status_published_at;
DROP INDEX idx_post_author_status_created_at;
DROP INDEX idx_post_status_created_at;

CREATE INDEX idx_post_lifecycle_moderation_published_at
    ON post(lifecycle_status, moderation_status, published_at);

CREATE INDEX idx_post_author_lifecycle_created_at
    ON post(author_id, lifecycle_status, created_at);

CREATE INDEX idx_post_lifecycle_deleted_at
    ON post(lifecycle_status, deleted_at);
