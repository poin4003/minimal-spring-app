-- Align catalog naming and add the supplier RBAC foundation.

ALTER TABLE post
    ALTER COLUMN type SET DATA TYPE VARCHAR(16);

DROP DOMAIN PostTypeEnum;

UPDATE post
SET type = 'CATALOG'
WHERE type = 'PRODUCT';

CREATE DOMAIN PostTypeEnum AS VARCHAR(16)
    CHECK (VALUE IN ('STANDARD', 'SHORT', 'VIDEO', 'CATALOG', 'WIKI', 'BLOG'));

ALTER TABLE post
    ALTER COLUMN type SET DATA TYPE PostTypeEnum;

INSERT INTO permission (id, name, permission_key)
SELECT
    'f1eebc99-9c0b-4ef8-bb6d-6bb9bd380f01',
    'Permission CATALOG:CREATE',
    'CATALOG:CREATE'
WHERE NOT EXISTS (
    SELECT 1 FROM permission
    WHERE permission_key = 'CATALOG:CREATE'
);

INSERT INTO permission (id, name, permission_key)
SELECT
    'f2eebc99-9c0b-4ef8-bb6d-6bb9bd380f02',
    'Permission CATALOG:VIEW:OWN',
    'CATALOG:VIEW:OWN'
WHERE NOT EXISTS (
    SELECT 1 FROM permission
    WHERE permission_key = 'CATALOG:VIEW:OWN'
);

INSERT INTO permission (id, name, permission_key)
SELECT
    'f3eebc99-9c0b-4ef8-bb6d-6bb9bd380f03',
    'Permission CATALOG:UPDATE:OWN',
    'CATALOG:UPDATE:OWN'
WHERE NOT EXISTS (
    SELECT 1 FROM permission
    WHERE permission_key = 'CATALOG:UPDATE:OWN'
);

INSERT INTO permission (id, name, permission_key)
SELECT
    'f4eebc99-9c0b-4ef8-bb6d-6bb9bd380f04',
    'Permission CATALOG:CONFIGURE',
    'CATALOG:CONFIGURE'
WHERE NOT EXISTS (
    SELECT 1 FROM permission
    WHERE permission_key = 'CATALOG:CONFIGURE'
);

INSERT INTO role (id, name, role_key)
SELECT
    'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380b03',
    'Supplier',
    'SUPPLIER'
WHERE NOT EXISTS (
    SELECT 1 FROM role
    WHERE role_key = 'SUPPLIER'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM role
CROSS JOIN permission
WHERE role.role_key = 'SUPER_ADMIN'
  AND permission.permission_key IN (
      'CATALOG:CREATE',
      'CATALOG:VIEW:OWN',
      'CATALOG:UPDATE:OWN',
      'CATALOG:CONFIGURE'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions existing
      WHERE existing.role_id = role.id
        AND existing.permission_id = permission.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM role
CROSS JOIN permission
WHERE role.role_key = 'SUPPLIER'
  AND permission.permission_key IN (
      'CATALOG:CREATE',
      'CATALOG:VIEW:OWN',
      'CATALOG:UPDATE:OWN',
      'MEDIA:VIEW:OWN',
      'MEDIA:MANAGE:OWN'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions existing
      WHERE existing.role_id = role.id
        AND existing.permission_id = permission.id
  );
