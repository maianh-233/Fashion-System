-- Add the Store authorization scope while preserving existing valid scope values.
ALTER TABLE role_permissions
  DROP CONSTRAINT chk_role_permissions_scope;

ALTER TABLE role_permissions
  ADD CONSTRAINT chk_role_permissions_scope
  CHECK (scope IN ('SELF', 'TEAM', 'DEPARTMENT', 'STORE', 'ALL'));

ALTER TABLE user_permissions
  DROP CONSTRAINT chk_user_permissions_scope;

ALTER TABLE user_permissions
  ADD CONSTRAINT chk_user_permissions_scope
  CHECK (scope IN ('SELF', 'TEAM', 'DEPARTMENT', 'STORE', 'ALL'));

-- Only the built-in Manager's Employee permissions become Store-scoped.
UPDATE role_permissions rp
SET scope = 'STORE'
FROM roles r
CROSS JOIN permissions p
JOIN permission_groups pg ON pg.id = p.group_id
WHERE rp.role_id = r.id
  AND p.id = rp.permission_id
  AND r.code = 'MANAGER'
  AND pg.code = 'EMPLOYEE'
  AND p.code IN ('USER_VIEW', 'USER_CREATE', 'USER_UPDATE', 'USER_DELETE');
