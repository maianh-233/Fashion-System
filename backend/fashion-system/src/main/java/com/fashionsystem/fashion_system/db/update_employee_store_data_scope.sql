-- =============================================================
-- LUNARIA - EMPLOYEE MANAGEMENT GLOBAL / STORE DATA SCOPE
-- PostgreSQL
-- =============================================================
--
-- Muc dich:
--   1. Bo sung STORE vao permission scope hien tai.
--   2. Dat cac Employee permission cua role MANAGER ve STORE.
--   3. Giu SUPER_ADMIN, ADMIN va cac role/custom grant khac nguyen trang.
--   4. Khong tao store_id tren users: Store cua employee duoc xac dinh
--      bang duy nhat mot ban ghi store_staffs active.
--
-- File nay dung de cap nhat DATABASE DANG TON TAI.
-- Co the chay lai an toan sau khi db.sql va seed_authorization.sql da chay.
-- =============================================================

BEGIN;

-- Khoa cac bang cau hinh permission trong luc thay constraint va scope.
LOCK TABLE role_permissions IN SHARE ROW EXCLUSIVE MODE;
LOCK TABLE user_permissions IN SHARE ROW EXCLUSIVE MODE;

-- -------------------------------------------------------------
-- 1. ROLE PERMISSION SCOPE
-- -------------------------------------------------------------
-- Cac database cu co the dung ten constraint mac dinh cua PostgreSQL.
ALTER TABLE role_permissions
  DROP CONSTRAINT IF EXISTS role_permissions_scope_check;

ALTER TABLE role_permissions
  DROP CONSTRAINT IF EXISTS chk_role_permissions_scope;

ALTER TABLE role_permissions
  ADD CONSTRAINT chk_role_permissions_scope
  CHECK (scope IN ('SELF', 'TEAM', 'DEPARTMENT', 'STORE', 'ALL'));

-- -------------------------------------------------------------
-- 2. USER PERMISSION SCOPE
-- -------------------------------------------------------------
-- Cac database cu co the dung ten constraint mac dinh cua PostgreSQL.
ALTER TABLE user_permissions
  DROP CONSTRAINT IF EXISTS user_permissions_scope_check;

ALTER TABLE user_permissions
  DROP CONSTRAINT IF EXISTS chk_user_permissions_scope;

ALTER TABLE user_permissions
  ADD CONSTRAINT chk_user_permissions_scope
  CHECK (scope IN ('SELF', 'TEAM', 'DEPARTMENT', 'STORE', 'ALL'));

-- -------------------------------------------------------------
-- 3. BUILT-IN MANAGER: EMPLOYEE MANAGEMENT = STORE SCOPE
-- -------------------------------------------------------------
-- Chi tac dong cac permission thuoc group EMPLOYEE.
-- Khong hardcode scope dua tren role trong backend; day chi la seed/config
-- mac dinh. Backend van resolve scope tu effective permission.
UPDATE role_permissions rp
SET scope = 'STORE'
FROM roles r
JOIN permissions p ON TRUE
JOIN permission_groups pg ON pg.id = p.group_id
WHERE rp.role_id = r.id
  AND rp.permission_id = p.id
  AND r.code = 'MANAGER'
  AND pg.code = 'EMPLOYEE'
  AND p.code IN (
    'USER_VIEW',
    'USER_CREATE',
    'USER_UPDATE',
    'USER_DELETE'
  );

COMMIT;

-- =============================================================
-- VERIFICATION - CAC QUERY BEN DUOI CHI DOC DU LIEU
-- =============================================================

-- A. Xem scope Employee cua tat ca role.
SELECT
  r.code AS role_code,
  r.name AS role_name,
  p.code AS permission_code,
  rp.scope
FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
JOIN permission_groups pg ON pg.id = p.group_id
WHERE pg.code = 'EMPLOYEE'
ORDER BY r.code, p.code;

-- B. Xem direct Employee permission cua user.
-- Direct ALLOW + ALL co the lam user co GLOBAL scope theo effective RBAC.
SELECT
  u.id AS user_id,
  u.username,
  u.email,
  p.code AS permission_code,
  up.effect,
  up.scope
FROM user_permissions up
JOIN users u ON u.id = up.user_id
JOIN permissions p ON p.id = up.permission_id
JOIN permission_groups pg ON pg.id = p.group_id
WHERE pg.code = 'EMPLOYEE'
ORDER BY u.username, p.code;

-- C. Tim MANAGER khong co chinh xac mot Store active.
-- Cac user STORE scope nay se bi backend tu choi de tranh leak du lieu.
SELECT
  u.id AS user_id,
  u.username,
  u.email,
  COUNT(DISTINCT ss.store_id)
    FILTER (WHERE ss.active = TRUE) AS active_store_count,
  ARRAY_AGG(DISTINCT ss.store_id)
    FILTER (WHERE ss.active = TRUE) AS active_store_ids
FROM users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r ON r.id = ur.role_id
LEFT JOIN store_staffs ss ON ss.user_id = u.id
WHERE r.code = 'MANAGER'
GROUP BY u.id, u.username, u.email
HAVING COUNT(DISTINCT ss.store_id)
         FILTER (WHERE ss.active = TRUE) <> 1
ORDER BY u.username;

-- D. Tim bat ky employee nao dang duoc gan nhieu Store active.
SELECT
  u.id AS user_id,
  u.username,
  COUNT(DISTINCT ss.store_id) AS active_store_count,
  ARRAY_AGG(DISTINCT ss.store_id ORDER BY ss.store_id) AS active_store_ids
FROM users u
JOIN store_staffs ss ON ss.user_id = u.id
WHERE ss.active = TRUE
GROUP BY u.id, u.username
HAVING COUNT(DISTINCT ss.store_id) > 1
ORDER BY u.username;

-- =============================================================
-- GAN STORE CHO EMPLOYEE - MAU CHAY THU CONG
-- =============================================================
-- Bo comment va thay USER_UUID / STORE_UUID bang UUID that.
-- Khong tu dong gan Store trong migration vi khong the suy ra Store dung.
--
-- BEGIN;
--
-- UPDATE store_staffs
-- SET active = FALSE,
--     end_date = CURRENT_DATE
-- WHERE user_id = 'USER_UUID'::uuid
--   AND active = TRUE
--   AND store_id <> 'STORE_UUID'::uuid;
--
-- INSERT INTO store_staffs (
--   id,
--   user_id,
--   store_id,
--   staff_role,
--   start_date,
--   active,
--   created_at
-- )
-- SELECT
--   gen_random_uuid(),
--   'USER_UUID'::uuid,
--   'STORE_UUID'::uuid,
--   'MANAGER',
--   CURRENT_DATE,
--   TRUE,
--   CURRENT_TIMESTAMP
-- WHERE EXISTS (
--   SELECT 1
--   FROM users
--   WHERE id = 'USER_UUID'::uuid
-- )
-- AND EXISTS (
--   SELECT 1
--   FROM stores
--   WHERE id = 'STORE_UUID'::uuid
--     AND active = TRUE
-- )
-- AND NOT EXISTS (
--   SELECT 1
--   FROM store_staffs
--   WHERE user_id = 'USER_UUID'::uuid
--     AND store_id = 'STORE_UUID'::uuid
--     AND active = TRUE
-- );
--
-- COMMIT;
