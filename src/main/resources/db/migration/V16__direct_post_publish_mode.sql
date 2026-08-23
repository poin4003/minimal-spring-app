-- Add a direct-publish policy without involving a human or AI moderator.

ALTER TABLE post_ai_moderation_config
    ALTER COLUMN mode SET DATA TYPE VARCHAR(16);

DROP DOMAIN PostAiModerationModeEnum;

CREATE DOMAIN PostAiModerationModeEnum AS VARCHAR(16)
    CONSTRAINT ck_post_ai_moderation_mode_value
    CHECK (VALUE IN ('MANUAL', 'AUTO', 'DIRECT_PUBLISH'));

ALTER TABLE post_ai_moderation_config
    ALTER COLUMN mode SET DATA TYPE PostAiModerationModeEnum;

ALTER TABLE post
    DROP CONSTRAINT ck_post_moderation_state;

ALTER TABLE post
    ALTER COLUMN moderation_source SET DATA TYPE VARCHAR(16);

DROP DOMAIN PostModerationSourceEnum;

CREATE DOMAIN PostModerationSourceEnum AS VARCHAR(16)
    CONSTRAINT ck_post_moderation_source_value
    CHECK (VALUE IN ('MANUAL', 'AI', 'DIRECT'));

ALTER TABLE post
    ALTER COLUMN moderation_source SET DATA TYPE PostModerationSourceEnum;

ALTER TABLE post
    ADD CONSTRAINT ck_post_moderation_state CHECK (
        (
            moderation_status IS NULL
            AND moderation_source IS NULL
            AND published_at IS NULL
            AND moderated_by_id IS NULL
            AND moderated_at IS NULL
            AND rejection_reason IS NULL
        )
        OR (
            moderation_status = 'PENDING_REVIEW'
            AND moderation_source IS NULL
            AND published_at IS NULL
            AND moderated_by_id IS NULL
            AND moderated_at IS NULL
            AND rejection_reason IS NULL
        )
        OR (
            moderation_status = 'PUBLISHED'
            AND moderation_source IS NOT NULL
            AND published_at IS NOT NULL
            AND moderated_at IS NOT NULL
            AND rejection_reason IS NULL
            AND (
                (
                    moderation_source = 'MANUAL'
                    AND moderated_by_id IS NOT NULL
                )
                OR (
                    moderation_source IN ('AI', 'DIRECT')
                    AND moderated_by_id IS NULL
                )
            )
        )
        OR (
            moderation_status = 'REJECTED'
            AND moderation_source IS NOT NULL
            AND published_at IS NULL
            AND moderated_at IS NOT NULL
            AND rejection_reason IS NOT NULL
            AND CHAR_LENGTH(TRIM(rejection_reason)) > 0
            AND (
                (
                    moderation_source = 'MANUAL'
                    AND moderated_by_id IS NOT NULL
                )
                OR (
                    moderation_source = 'AI'
                    AND moderated_by_id IS NULL
                )
            )
        )
    );
