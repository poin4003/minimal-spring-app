-- Bootstrap data for the minimal Spring Boot project.
-- Includes permissions, roles, root admin account, and recurring job configs.

INSERT INTO permission (id, name, permission_key) VALUES
    ('ecacccdf-f953-4166-a80c-9ddad96fb2c2', 'Permission RBAC:MANAGE', 'RBAC:MANAGE'),
    ('2cb91645-eb69-4582-b389-c34ac3d5dd72', 'Permission SIM:IMPORT', 'SIM:IMPORT'),
    ('dbfab2cd-a9b2-4836-8f21-9fea9efe2850', 'Permission SIM:EXPORT', 'SIM:EXPORT'),
    ('ecb6e665-2822-47cf-996c-80b1f2a21c0c', 'Permission SIM:UPDATE', 'SIM:UPDATE'),
    ('0b952be2-f68d-43de-9e1a-0e7663872bd3', 'Permission SIM:CREATE', 'SIM:CREATE'),
    ('5427f69b-4762-408d-865d-191b3502df2f', 'Permission SIM:VIEW', 'SIM:VIEW'),
    ('f4d6e429-2359-4b47-9245-194948d3fa79', 'Permission USER:CREATE', 'USER:CREATE'),
    ('8871ca39-7ee7-4539-a081-e9eeedb66f31', 'Permission USER:VIEW', 'USER:VIEW'),
    ('c1eebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 'Permission CRONJOB:VIEW', 'CRONJOB:VIEW'),
    ('c2eebc99-9c0b-4ef8-bb6d-6bb9bd380c02', 'Permission CRONJOB:UPDATE', 'CRONJOB:UPDATE');

INSERT INTO role (id, name, role_key) VALUES (
    'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b01',
    'Super Admin',
    'SUPER_ADMIN'
);

INSERT INTO user_base (id, email, password, status) VALUES (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
    '${bootstrapAdminEmail}',
    '${bootstrapAdminPasswordHash}',
    'ACTIVE'
);

INSERT INTO user_info (id, username) VALUES (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
    '${bootstrapAdminName}'
);

INSERT INTO user_roles (user_id, role_id) VALUES (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01',
    'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b01'
);

INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b01', 'ecacccdf-f953-4166-a80c-9ddad96fb2c2'),
    ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b01', '2cb91645-eb69-4582-b389-c34ac3d5dd72'),
    ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b01', 'dbfab2cd-a9b2-4836-8f21-9fea9efe2850'),
    ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b01', 'ecb6e665-2822-47cf-996c-80b1f2a21c0c'),
    ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b01', '0b952be2-f68d-43de-9e1a-0e7663872bd3'),
    ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b01', '5427f69b-4762-408d-865d-191b3502df2f'),
    ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b01', 'f4d6e429-2359-4b47-9245-194948d3fa79'),
    ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b01', '8871ca39-7ee7-4539-a081-e9eeedb66f31'),
    ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b01', 'c1eebc99-9c0b-4ef8-bb6d-6bb9bd380c01'),
    ('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b01', 'c2eebc99-9c0b-4ef8-bb6d-6bb9bd380c02');

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
