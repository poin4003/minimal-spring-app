-- Add short-post details and persist original media metadata for short policies.

CREATE TABLE short_post (
    post_id UUID PRIMARY KEY,
    caption VARCHAR(1000),
    CONSTRAINT fk_short_post_post
        FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE
);

ALTER TABLE media
    ADD COLUMN original_width INTEGER;

ALTER TABLE media
    ADD COLUMN original_height INTEGER;

ALTER TABLE media
    ADD COLUMN duration_millis BIGINT;

ALTER TABLE media
    ADD CONSTRAINT ck_media_original_dimensions CHECK (
        (
            original_width IS NULL
            AND original_height IS NULL
        )
        OR (
            original_width > 0
            AND original_height > 0
        )
    );

ALTER TABLE media
    ADD CONSTRAINT ck_media_duration CHECK (
        duration_millis IS NULL
        OR duration_millis > 0
    );
