-- Permission catalog cho trang quản lý cửa hàng. Có thể chạy độc lập sau V6.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO permission_groups (id, module_id, code, name, description, created_at)
SELECT gen_random_uuid(), m.id, 'STORE', 'Cửa hàng',
       'Quản lý cửa hàng và địa điểm phân công nhân viên', CURRENT_TIMESTAMP
FROM modules m WHERE m.code = 'HUMAN_RESOURCE'
ON CONFLICT (code) DO UPDATE SET module_id = EXCLUDED.module_id,
  name = EXCLUDED.name, description = EXCLUDED.description;

WITH store_permissions(code, name, description) AS (
  VALUES
    ('STORE_VIEW',   'Xem cửa hàng', 'Xem danh sách và chi tiết cửa hàng'),
    ('STORE_CREATE', 'Tạo cửa hàng', 'Tạo cửa hàng mới'),
    ('STORE_UPDATE', 'Sửa cửa hàng', 'Cập nhật thông tin và trạng thái cửa hàng'),
    ('STORE_DELETE', 'Xóa cửa hàng', 'Xóa cửa hàng chưa có dữ liệu tham chiếu')
)
INSERT INTO permissions (id, group_id, code, name, description, created_at)
SELECT gen_random_uuid(), pg.id, sp.code, sp.name, sp.description, CURRENT_TIMESTAMP
FROM permission_groups pg CROSS JOIN store_permissions sp WHERE pg.code = 'STORE'
ON CONFLICT (code) DO UPDATE SET group_id = EXCLUDED.group_id,
  name = EXCLUDED.name, description = EXCLUDED.description;

DELETE FROM role_permissions rp USING roles r, permissions p
WHERE rp.role_id = r.id AND rp.permission_id = p.id
  AND p.code IN ('STORE_VIEW', 'STORE_CREATE', 'STORE_UPDATE', 'STORE_DELETE')
  AND r.code NOT IN ('ADMIN', 'SUPER_ADMIN');

INSERT INTO role_permissions (role_id, permission_id, scope)
SELECT r.id, p.id, 'ALL' FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('ADMIN', 'SUPER_ADMIN')
  AND p.code IN ('STORE_VIEW', 'STORE_CREATE', 'STORE_UPDATE', 'STORE_DELETE')
ON CONFLICT (role_id, permission_id) DO UPDATE SET scope = EXCLUDED.scope;
