-- New posts start as drafts and enter moderation only after explicit submission.

ALTER TABLE post
    ALTER COLUMN lifecycle_status SET DEFAULT 'DRAFT';
