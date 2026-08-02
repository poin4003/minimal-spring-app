-- RBAC data for post creation, owner access, and moderation.

INSERT INTO permission (id, name, permission_key) VALUES
    (
        'e1eebc99-9c0b-4ef8-bb6d-6bb9bd380e01',
        'Permission POST:CREATE',
        'POST:CREATE'
    ),
    (
        'e2eebc99-9c0b-4ef8-bb6d-6bb9bd380e02',
        'Permission POST:VIEW:OWN',
        'POST:VIEW:OWN'
    ),
    (
        'e3eebc99-9c0b-4ef8-bb6d-6bb9bd380e03',
        'Permission POST:MODERATE',
        'POST:MODERATE'
    );

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM role
CROSS JOIN permission
WHERE role.role_key = 'SUPER_ADMIN'
  AND permission.permission_key IN (
      'POST:CREATE',
      'POST:VIEW:OWN',
      'POST:MODERATE'
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM role
CROSS JOIN permission
WHERE role.role_key = 'USER'
  AND permission.permission_key IN (
      'POST:CREATE',
      'POST:VIEW:OWN'
  );
