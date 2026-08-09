-- Add lifecycle cleanup support for deleted and rejected posts.

CREATE INDEX idx_post_lifecycle_moderation_moderated_at
    ON post(lifecycle_status, moderation_status, moderated_at);

INSERT INTO cronjob_config (id, job_type, expression, status) VALUES (
    'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c10',
    'CLEANUP_POSTS',
    NULL,
    'ACTIVE'
);
