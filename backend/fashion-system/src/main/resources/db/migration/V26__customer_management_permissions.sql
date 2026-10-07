BEGIN;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM modules WHERE code='CUSTOMER') THEN
        RAISE EXCEPTION 'V26: CUSTOMER module must exist before customer permissions are installed';
    END IF;
END $$;

INSERT INTO permission_groups(id,module_id,name,code,description,created_at)
SELECT (
    substr(md5('permission-group:CUSTOMER'),1,8) || '-' || substr(md5('permission-group:CUSTOMER'),9,4) || '-' ||
    substr(md5('permission-group:CUSTOMER'),13,4) || '-' || substr(md5('permission-group:CUSTOMER'),17,4) || '-' ||
    substr(md5('permission-group:CUSTOMER'),21,12)
)::uuid, m.id, 'Khách hàng', 'CUSTOMER', 'Quản lý hồ sơ khách hàng', current_timestamp
FROM modules m WHERE m.code='CUSTOMER'
ON CONFLICT(code) DO NOTHING;

INSERT INTO permissions(id,code,name,group_id,description,created_at)
SELECT (
    substr(md5('permission:' || action.code),1,8) || '-' || substr(md5('permission:' || action.code),9,4) || '-' ||
    substr(md5('permission:' || action.code),13,4) || '-' || substr(md5('permission:' || action.code),17,4) || '-' ||
    substr(md5('permission:' || action.code),21,12)
)::uuid, action.code, action.name, g.id, action.description, current_timestamp
FROM permission_groups g
CROSS JOIN (VALUES
    ('CUSTOMER_VIEW','Xem khách hàng','Xem, tìm kiếm và tra cứu khách hàng theo scope'),
    ('CUSTOMER_CREATE','Tạo khách hàng','Tạo Store Member theo scope'),
    ('CUSTOMER_UPDATE','Cập nhật khách hàng','Cập nhật Store Member theo scope'),
    ('CUSTOMER_STATUS_MANAGE','Quản lý trạng thái khách hàng','Kích hoạt hoặc ngưng khách hàng theo scope'),
    ('CUSTOMER_TIER_MANAGE','Quản lý hạng khách hàng','Thay đổi hạng khách hàng ở phạm vi toàn chuỗi')
) action(code,name,description)
WHERE g.code='CUSTOMER'
ON CONFLICT(code) DO NOTHING;

INSERT INTO role_permissions(role_id,permission_id,scope)
SELECT r.id,p.id,'ALL' FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('SUPER_ADMIN','ADMIN') AND p.code LIKE 'CUSTOMER_%'
ON CONFLICT(role_id,permission_id) DO NOTHING;

INSERT INTO role_permissions(role_id,permission_id,scope)
SELECT r.id,p.id,'STORE' FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('MANAGER','STAFF')
  AND p.code IN ('CUSTOMER_VIEW','CUSTOMER_CREATE','CUSTOMER_UPDATE','CUSTOMER_STATUS_MANAGE')
ON CONFLICT(role_id,permission_id) DO NOTHING;

COMMIT;
