-- Allow post owners to edit their own posts and submit them for review again.

INSERT INTO permission (id, name, permission_key) VALUES
    (
        'e4eebc99-9c0b-4ef8-bb6d-6bb9bd380e04',
        'Permission POST:UPDATE:OWN',
        'POST:UPDATE:OWN'
    );

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM role
CROSS JOIN permission
WHERE role.role_key IN ('SUPER_ADMIN', 'USER')
  AND permission.permission_key = 'POST:UPDATE:OWN';
