-- Schema for the post aggregate and standard post detail.

CREATE DOMAIN PostTypeEnum AS VARCHAR(16)
    CHECK (VALUE IN ('STANDARD', 'SHORT', 'MOVIE', 'PRODUCT', 'WIKI', 'BLOG'));

CREATE DOMAIN PostModerationStatusEnum AS VARCHAR(16)
    CHECK (VALUE IN ('PENDING_REVIEW', 'PUBLISHED', 'REJECTED'));

CREATE DOMAIN PostMediaRoleEnum AS VARCHAR(16)
    CHECK (VALUE IN ('COVER', 'CONTENT', 'GALLERY', 'TRAILER', 'EPISODE', 'SOURCE'));

CREATE TABLE post (
    id UUID PRIMARY KEY,
    author_id UUID NOT NULL,
    type PostTypeEnum NOT NULL,
    moderation_status PostModerationStatusEnum NOT NULL,
    published_at TIMESTAMP,
    moderated_by_id UUID,
    moderated_at TIMESTAMP,
    rejection_reason VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_post_author
        FOREIGN KEY (author_id) REFERENCES user_base(id),
    CONSTRAINT fk_post_moderated_by
        FOREIGN KEY (moderated_by_id) REFERENCES user_base(id),
    CONSTRAINT ck_post_moderation_state CHECK (
        (
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
    )
);

CREATE INDEX idx_post_status_published_at
    ON post(moderation_status, published_at);
CREATE INDEX idx_post_author_status_created_at
    ON post(author_id, moderation_status, created_at);
CREATE INDEX idx_post_status_created_at
    ON post(moderation_status, created_at);

CREATE TABLE standard_post (
    post_id UUID PRIMARY KEY,
    content TEXT,
    CONSTRAINT fk_standard_post_post
        FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE
);

CREATE TABLE post_media (
    id UUID PRIMARY KEY,
    post_id UUID NOT NULL,
    media_id UUID NOT NULL,
    role PostMediaRoleEnum NOT NULL,
    position INTEGER NOT NULL,
    CONSTRAINT fk_post_media_post
        FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE,
    CONSTRAINT fk_post_media_media
        FOREIGN KEY (media_id) REFERENCES media(id),
    CONSTRAINT ck_post_media_position CHECK (position >= 0)
);

CREATE UNIQUE INDEX uk_post_media_post_role_position
    ON post_media(post_id, role, position);
CREATE UNIQUE INDEX uk_post_media_post_media
    ON post_media(post_id, media_id);
CREATE INDEX idx_post_media_media_id ON post_media(media_id);
