-- Refactor permission tree to Module -> PermissionGroup -> Permission.
-- Existing grants preserve their previous unrestricted behavior by defaulting to ALL.

CREATE TABLE modules (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  code VARCHAR(50) UNIQUE NOT NULL,
  name VARCHAR(100) NOT NULL,
  description TEXT,
  icon VARCHAR(100),
  sort_order INTEGER NOT NULL DEFAULT 0,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO modules (code, name, description, icon, sort_order)
VALUES ('SYSTEM', 'System', 'Permission groups migrated from the legacy schema', 'settings', 0);

ALTER TABLE permission_groups ADD COLUMN module_id UUID;

UPDATE permission_groups
   SET module_id = (SELECT id FROM modules WHERE code = 'SYSTEM')
 WHERE module_id IS NULL;

ALTER TABLE permission_groups ALTER COLUMN module_id SET NOT NULL;
ALTER TABLE permission_groups
  ADD CONSTRAINT fk_permission_groups_module
  FOREIGN KEY (module_id) REFERENCES modules(id) ON DELETE RESTRICT;
CREATE INDEX idx_permission_groups_module_id ON permission_groups(module_id);

-- Legacy permissions were nullable. Put any orphan in a real group before enforcing the hierarchy.
INSERT INTO permission_groups (module_id, code, name, description)
SELECT id, 'GENERAL', 'General', 'Migrated permissions without a group'
  FROM modules
 WHERE code = 'SYSTEM'
   AND EXISTS (SELECT 1 FROM permissions WHERE group_id IS NULL);

UPDATE permissions
   SET group_id = (SELECT id FROM permission_groups WHERE code = 'GENERAL')
 WHERE group_id IS NULL;

ALTER TABLE permissions DROP CONSTRAINT fk_permissions_group;
ALTER TABLE permissions ALTER COLUMN group_id SET NOT NULL;
ALTER TABLE permissions
  ADD CONSTRAINT fk_permissions_group
  FOREIGN KEY (group_id) REFERENCES permission_groups(id) ON DELETE RESTRICT;

ALTER TABLE role_permissions
  ADD COLUMN scope VARCHAR(20) NOT NULL DEFAULT 'ALL';
ALTER TABLE role_permissions
  ADD CONSTRAINT chk_role_permissions_scope
  CHECK (scope IN ('SELF', 'TEAM', 'DEPARTMENT', 'ALL'));

ALTER TABLE user_permissions
  ADD COLUMN effect VARCHAR(10) NOT NULL DEFAULT 'ALLOW',
  ADD COLUMN scope VARCHAR(20) NOT NULL DEFAULT 'ALL';
ALTER TABLE user_permissions
  ADD CONSTRAINT chk_user_permissions_effect CHECK (effect IN ('ALLOW', 'DENY')),
  ADD CONSTRAINT chk_user_permissions_scope CHECK (scope IN ('SELF', 'TEAM', 'DEPARTMENT', 'ALL'));

CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);
CREATE INDEX idx_user_permissions_permission_id ON user_permissions(permission_id);
