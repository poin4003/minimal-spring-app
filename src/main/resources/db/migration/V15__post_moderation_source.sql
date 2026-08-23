-- Distinguish human moderation from automated AI decisions.

CREATE DOMAIN PostModerationSourceEnum AS VARCHAR(16)
    CHECK (VALUE IN ('MANUAL', 'AI'));

ALTER TABLE post
    ADD COLUMN moderation_source PostModerationSourceEnum;

UPDATE post
SET moderation_source = 'MANUAL'
WHERE moderation_status IN ('PUBLISHED', 'REJECTED');

ALTER TABLE post
    DROP CONSTRAINT ck_post_moderation_state;

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
                    moderation_source = 'AI'
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
