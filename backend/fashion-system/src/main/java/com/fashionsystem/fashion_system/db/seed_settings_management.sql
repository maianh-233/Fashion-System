-- =============================================================
-- LUNARIA - SEED QUAN LY SETTING PHAN QUYEN (POSTGRESQL)
-- =============================================================
-- Chay sau db.sql/migration va sau khi cac role he thong da ton tai.
-- Script idempotent: co the chay lai ma khong tao ban ghi trung.
--
-- ROLE CANONICAL CUA HE THONG:
--   SUPER_ADMIN : quan tri vien toi cao, xem/sua toan bo store va nhan vien.
--   ADMIN       : quan tri vien, xem/sua toan bo store va nhan vien.
--   MANAGER     : quan ly cua hang; chi xem/sua nhan vien co store_staffs trung store.
--   ACCOUNTANT  : ke toan.
--   STAFF       : nhan vien ban hang.
--   WAREHOUSE   : nhan vien kho (khong dung WARE_HOUSE).
-- Gioi han nhan vien theo store duoc backend /api/employees ap dung theo principal;
-- khong tin storeId do frontend gui len.

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO modules (id, code, name, description, icon, sort_order, active, created_at)
VALUES (
  gen_random_uuid(), 'SYSTEM', 'Hệ thống',
  'Vai trò, quyền và cấu hình hệ thống', 'settings', 70, TRUE, CURRENT_TIMESTAMP
)
ON CONFLICT (code) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  icon = EXCLUDED.icon,
  sort_order = EXCLUDED.sort_order,
  active = EXCLUDED.active;

INSERT INTO permission_groups (id, module_id, code, name, description, created_at)
SELECT
  gen_random_uuid(), m.id, 'SETTINGS', 'Cấu hình hệ thống',
  'Quản lý module, nhóm quyền và permission', CURRENT_TIMESTAMP
FROM modules m
WHERE m.code = 'SYSTEM'
ON CONFLICT (code) DO UPDATE SET
  module_id = EXCLUDED.module_id,
  name = EXCLUDED.name,
  description = EXCLUDED.description;

INSERT INTO permissions (id, group_id, code, name, description, created_at)
SELECT
  gen_random_uuid(), pg.id, 'SETTINGS_MANAGE', 'Quản lý cấu hình quyền',
  'Cho phép quản lý catalog module, permission group và permission', CURRENT_TIMESTAMP
FROM permission_groups pg
WHERE pg.code = 'SETTINGS'
ON CONFLICT (code) DO UPDATE SET
  group_id = EXCLUDED.group_id,
  name = EXCLUDED.name,
  description = EXCLUDED.description;

INSERT INTO role_permissions (role_id, permission_id, scope)
SELECT r.id, p.id, 'ALL'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'SUPER_ADMIN')
  AND p.code = 'SETTINGS_MANAGE'
ON CONFLICT (role_id, permission_id) DO UPDATE SET
  scope = EXCLUDED.scope;

-- -------------------------------------------------------------
-- QUAN LY CUA HANG
-- Group nay nam trong module HUMAN_RESOURCE de hien thi cung Nhan su.
-- Chi ADMIN/SUPER_ADMIN duoc cap permission nen cac role khac khong thay
-- menu, khong vao duoc route va cung khong goi duoc API /api/admin/stores.
-- -------------------------------------------------------------
INSERT INTO permission_groups (id, module_id, code, name, description, created_at)
SELECT
  gen_random_uuid(), m.id, 'STORE', 'Cửa hàng',
  'Quản lý cửa hàng và địa điểm phân công nhân viên', CURRENT_TIMESTAMP
FROM modules m
WHERE m.code = 'HUMAN_RESOURCE'
ON CONFLICT (code) DO UPDATE SET
  module_id = EXCLUDED.module_id,
  name = EXCLUDED.name,
  description = EXCLUDED.description;

WITH store_permissions(code, name, description) AS (
  VALUES
    ('STORE_VIEW',   'Xem cửa hàng', 'Xem danh sách và chi tiết cửa hàng'),
    ('STORE_CREATE', 'Tạo cửa hàng', 'Tạo cửa hàng mới'),
    ('STORE_UPDATE', 'Sửa cửa hàng', 'Cập nhật thông tin và trạng thái cửa hàng'),
    ('STORE_DELETE', 'Xóa cửa hàng', 'Xóa cửa hàng chưa có dữ liệu tham chiếu')
)
INSERT INTO permissions (id, group_id, code, name, description, created_at)
SELECT gen_random_uuid(), pg.id, sp.code, sp.name, sp.description, CURRENT_TIMESTAMP
FROM permission_groups pg
CROSS JOIN store_permissions sp
WHERE pg.code = 'STORE'
ON CONFLICT (code) DO UPDATE SET
  group_id = EXCLUDED.group_id,
  name = EXCLUDED.name,
  description = EXCLUDED.description;

-- Xoa grant STORE neu tung bi cap nham cho role nghiep vu.
DELETE FROM role_permissions rp
USING roles r, permissions p
WHERE rp.role_id = r.id
  AND rp.permission_id = p.id
  AND p.code IN ('STORE_VIEW', 'STORE_CREATE', 'STORE_UPDATE', 'STORE_DELETE')
  AND r.code NOT IN ('ADMIN', 'SUPER_ADMIN');

INSERT INTO role_permissions (role_id, permission_id, scope)
SELECT r.id, p.id, 'ALL'
FROM roles r
CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'SUPER_ADMIN')
  AND p.code IN ('STORE_VIEW', 'STORE_CREATE', 'STORE_UPDATE', 'STORE_DELETE')
ON CONFLICT (role_id, permission_id) DO UPDATE SET scope = EXCLUDED.scope;

COMMIT;

-- Kiem tra:
-- SELECT r.code AS role_code, p.code AS permission_code, rp.scope
-- FROM role_permissions rp
-- JOIN roles r ON r.id = rp.role_id
-- JOIN permissions p ON p.id = rp.permission_id
-- WHERE p.code = 'SETTINGS_MANAGE'
-- ORDER BY r.code;
--
-- SELECT r.code AS role_code, p.code AS permission_code
-- FROM role_permissions rp
-- JOIN roles r ON r.id = rp.role_id
-- JOIN permissions p ON p.id = rp.permission_id
-- WHERE p.code LIKE 'STORE_%'
-- ORDER BY r.code, p.code;
