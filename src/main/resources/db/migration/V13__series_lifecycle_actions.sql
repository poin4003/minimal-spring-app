-- Track archive actions so series cascades restore only affected videos.

ALTER TABLE post
    ADD COLUMN archived_at TIMESTAMP;

UPDATE post
SET archived_at = updated_at
WHERE lifecycle_status = 'ARCHIVED';

ALTER TABLE post
    DROP CONSTRAINT ck_post_lifecycle_state;

ALTER TABLE post
    ADD CONSTRAINT ck_post_lifecycle_state CHECK (
        (
            lifecycle_status = 'DRAFT'
            AND archived_at IS NULL
            AND deleted_at IS NULL
        )
        OR (
            lifecycle_status = 'ACTIVE'
            AND moderation_status IS NOT NULL
            AND archived_at IS NULL
            AND deleted_at IS NULL
        )
        OR (
            lifecycle_status = 'ARCHIVED'
            AND moderation_status = 'PUBLISHED'
            AND archived_at IS NOT NULL
            AND deleted_at IS NULL
        )
        OR (
            lifecycle_status = 'DELETED'
            AND archived_at IS NULL
            AND deleted_at IS NOT NULL
        )
    );

ALTER TABLE video_series
    ADD COLUMN archived_at TIMESTAMP;

ALTER TABLE video_series
    DROP CONSTRAINT ck_video_series_lifecycle_state;

ALTER TABLE video_series
    ADD CONSTRAINT ck_video_series_lifecycle_state CHECK (
        (
            lifecycle_status = 'ACTIVE'
            AND archived_at IS NULL
            AND deleted_at IS NULL
        )
        OR (
            lifecycle_status = 'ARCHIVED'
            AND archived_at IS NOT NULL
            AND deleted_at IS NULL
        )
        OR (
            lifecycle_status = 'DELETED'
            AND archived_at IS NULL
            AND deleted_at IS NOT NULL
        )
    );
