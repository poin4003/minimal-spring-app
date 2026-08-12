-- Add independent video posts and optional YouTube-style series grouping.

ALTER TABLE post
    ALTER COLUMN type SET DATA TYPE VARCHAR(16);

DROP DOMAIN PostTypeEnum;

UPDATE post
SET type = 'VIDEO'
WHERE type = 'MOVIE';

CREATE DOMAIN PostTypeEnum AS VARCHAR(16)
    CHECK (VALUE IN ('STANDARD', 'SHORT', 'VIDEO', 'PRODUCT', 'WIKI', 'BLOG'));

ALTER TABLE post
    ALTER COLUMN type SET DATA TYPE PostTypeEnum;

CREATE INDEX idx_post_type_lifecycle_moderation_published_at
    ON post(type, lifecycle_status, moderation_status, published_at);

CREATE TABLE video_post (
    post_id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    CONSTRAINT fk_video_post_post
        FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE
);

CREATE TABLE video_series (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    cover_media_id UUID,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    video_count INTEGER DEFAULT 0 NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_video_series_owner
        FOREIGN KEY (owner_id) REFERENCES user_base(id),
    CONSTRAINT fk_video_series_cover_media
        FOREIGN KEY (cover_media_id) REFERENCES media(id) ON DELETE SET NULL,
    CONSTRAINT ck_video_series_video_count CHECK (video_count >= 0)
);

CREATE INDEX idx_video_series_owner_created_at
    ON video_series(owner_id, created_at);

CREATE TABLE video_series_item (
    id UUID PRIMARY KEY,
    series_id UUID NOT NULL,
    video_post_id UUID NOT NULL,
    position INTEGER NOT NULL,
    CONSTRAINT fk_video_series_item_series
        FOREIGN KEY (series_id) REFERENCES video_series(id) ON DELETE CASCADE,
    CONSTRAINT fk_video_series_item_video_post
        FOREIGN KEY (video_post_id) REFERENCES video_post(post_id) ON DELETE CASCADE,
    CONSTRAINT ck_video_series_item_position CHECK (position >= 0)
);

CREATE UNIQUE INDEX uk_video_series_item_position
    ON video_series_item(series_id, position);

CREATE UNIQUE INDEX uk_video_series_item_video
    ON video_series_item(series_id, video_post_id);

CREATE INDEX idx_video_series_item_video_post
    ON video_series_item(video_post_id);
