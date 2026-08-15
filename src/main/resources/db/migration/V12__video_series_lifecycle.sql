-- Add an owner-controlled lifecycle to video series.

CREATE DOMAIN VideoSeriesLifecycleStatusEnum AS VARCHAR(16)
    CHECK (VALUE IN ('ACTIVE', 'ARCHIVED', 'DELETED'));

ALTER TABLE video_series
    ADD COLUMN lifecycle_status VideoSeriesLifecycleStatusEnum
        DEFAULT 'ACTIVE' NOT NULL;

ALTER TABLE video_series
    ADD COLUMN deleted_at TIMESTAMP;

ALTER TABLE video_series
    ADD CONSTRAINT ck_video_series_lifecycle_state CHECK (
        (
            lifecycle_status IN ('ACTIVE', 'ARCHIVED')
            AND deleted_at IS NULL
        )
        OR (
            lifecycle_status = 'DELETED'
            AND deleted_at IS NOT NULL
        )
    );

DROP INDEX idx_video_series_owner_created_at;

CREATE INDEX idx_video_series_owner_lifecycle_created_at
    ON video_series(owner_id, lifecycle_status, created_at);

CREATE INDEX idx_video_series_lifecycle_created_at
    ON video_series(lifecycle_status, created_at);

CREATE INDEX idx_video_series_lifecycle_deleted_at
    ON video_series(lifecycle_status, deleted_at);
