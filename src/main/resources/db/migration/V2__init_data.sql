-- Bootstrap data for the minimal Spring Boot project.
-- Includes permissions, roles, root admin account, and recurring job configs.

INSERT INTO permission (id, name, permission_key) VALUES
    ('ecacccdf-f953-4166-a80c-9ddad96fb2c2', 'Permission RBAC:MANAGE', 'RBAC:MANAGE'),
    ('f4d6e429-2359-4b47-9245-194948d3fa79', 'Permission USER:CREATE', 'USER:CREATE'),
    ('8871ca39-7ee7-4539-a081-e9eeedb66f31', 'Permission USER:VIEW', 'USER:VIEW'),
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 'Permission CRONJOB:VIEW', 'CRONJOB:VIEW'),
    ('c2eebc99-9c0b-4ef8-bb6d-6bb9bd380c02', 'Permission CRONJOB:UPDATE', 'CRONJOB:UPDATE'),
    ('d1eebc99-9c0b-4ef8-bb6d-6bb9bd380d01', 'Permission MEDIA:VIEW', 'MEDIA:VIEW'),
    ('d2eebc99-9c0b-4ef8-bb6d-6bb9bd380d02', 'Permission MEDIA:MANAGE', 'MEDIA:MANAGE'),
    ('d3eebc99-9c0b-4ef8-bb6d-6bb9bd380d03', 'Permission MEDIA:VIEW:OWN', 'MEDIA:VIEW:OWN'),
    ('d4eebc99-9c0b-4ef8-bb6d-6bb9bd380d04', 'Permission MEDIA:MANAGE:OWN', 'MEDIA:MANAGE:OWN');

INSERT INTO role (id, name, role_key) VALUES (
    'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b01',
    'Super Admin',
    'SUPER_ADMIN'
);

INSERT INTO role (id, name, role_key) VALUES (
    'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b02',
    'User',
    'USER'
);

INSERT INTO user_base (id, email, password, status) VALUES (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
    '${bootstrapAdminEmail}',
    '${bootstrapAdminPasswordHash}',
    'ACTIVE'
);

INSERT INTO user_info (
    id,
    full_name,
    language,
    dark_theme_enabled
) VALUES (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
    '${bootstrapAdminName}',
    'EN',
    FALSE
);

INSERT INTO user_roles (user_id, role_id) VALUES (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
    'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b01'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM role
CROSS JOIN permission
WHERE role.role_key = 'SUPER_ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM role
CROSS JOIN permission
WHERE role.role_key = 'USER'
  AND permission.permission_key IN (
      'USER:VIEW',
      'MEDIA:VIEW',
      'MEDIA:VIEW:OWN',
      'MEDIA:MANAGE:OWN'
  );

INSERT INTO cronjob_config (id, job_type, expression, status) VALUES (
    'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c01',
    'CLEANUP_EXPIRED_TOKENS',
    NULL,
    'ACTIVE'
);

INSERT INTO cronjob_config (id, job_type, expression, status) VALUES (
    'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c02',
    'RECOVER_PENDING_MEDIA',
    NULL,
    'ACTIVE'
);

INSERT INTO cronjob_config (id, job_type, expression, status) VALUES (
    'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c03',
    'CLEANUP_KNOWN_MEDIA',
    NULL,
    'ACTIVE'
);

INSERT INTO cronjob_config (id, job_type, expression, status) VALUES (
    'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c04',
    'CLEANUP_MEDIA_STORAGE',
    NULL,
    'ACTIVE'
);

INSERT INTO cronjob_config (id, job_type, expression, status) VALUES (
    'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c05',
    'CLEANUP_MEDIA_UPLOADS',
    NULL,
    'ACTIVE'
);

INSERT INTO cronjob_config (id, job_type, expression, status) VALUES (
    'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c06',
    'CLEANUP_JOBRUNR_FAILED_JOBS',
    NULL,
    'ACTIVE'
);

INSERT INTO cronjob_config (id, job_type, expression, status) VALUES (
    'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c07',
    'CLEANUP_NOTIFICATIONS',
    NULL,
    'ACTIVE'
);

INSERT INTO cronjob_config (id, job_type, expression, status) VALUES (
    'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c08',
    'CLEANUP_REGISTRATIONS',
    NULL,
    'ACTIVE'
);

INSERT INTO cronjob_config (id, job_type, expression, status) VALUES (
    'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380c09',
    'CLEANUP_PASSWORD_RESETS',
    NULL,
    'ACTIVE'
);
