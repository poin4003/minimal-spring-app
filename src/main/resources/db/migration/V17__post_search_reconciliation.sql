-- Register periodic recovery and backfill for the optional Lucene projection.

INSERT INTO cronjob_config (id, job_type, expression, status) VALUES (
    'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c11',
    'RECONCILE_POST_SEARCH_INDEX',
    NULL,
    'ACTIVE'
);
