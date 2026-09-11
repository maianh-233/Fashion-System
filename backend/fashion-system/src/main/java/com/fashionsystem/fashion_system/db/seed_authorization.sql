-- =============================================================
-- LUNARIA - DU LIEU KHOI TAO PHAN QUYEN (POSTGRESQL)
-- =============================================================
-- Chay file nay SAU db.sql (hoac sau migration V9).
-- File co the chay lai: catalog duoc upsert, cac bang lien ket
-- dung ON CONFLICT va mat khau cua tai khoan da ton tai khong bi doi.
--
-- Module          -> link tren header admin
-- PermissionGroup -> link tren sidebar admin
-- Permission      -> action/button va authority cua backend
-- RolePermission  -> permission hieu luc cua tung role
--
-- QUAN TRONG: an button/menu khong thay the authorization o backend.
-- Moi API nghiep vu van phai dung @PreAuthorize/AuthorizationService va
-- loc du lieu theo scope truoc khi doc hoac thay doi bang.
-- =============================================================

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- -------------------------------------------------------------
-- 1. MODULES - thu tu va code khop adminNavigation.js
-- -------------------------------------------------------------
INSERT INTO modules (id, code, name, description, icon, sort_order, active, created_at)
VALUES
  (gen_random_uuid(), 'OVERVIEW',       'Tổng quan', 'Dashboard và thông tin tổng quan hệ thống',       'house',        10, TRUE, CURRENT_TIMESTAMP),
  (gen_random_uuid(), 'CATALOG',        'Sản phẩm',  'Danh mục, sản phẩm và tồn kho',                    'shirt',        20, TRUE, CURRENT_TIMESTAMP),
  (gen_random_uuid(), 'SALES',          'Bán hàng',  'Đơn hàng, khách hàng và khuyến mãi',               'shopping-bag', 30, TRUE, CURRENT_TIMESTAMP),
  (gen_random_uuid(), 'HUMAN_RESOURCE', 'Nhân sự',   'Nhân viên, công việc và hiệu suất',                'users',        40, TRUE, CURRENT_TIMESTAMP),
  (gen_random_uuid(), 'WAREHOUSE',      'Kho vận',   'Nhập hàng, xuất hàng và nhà cung cấp',             'warehouse',    50, TRUE, CURRENT_TIMESTAMP),
  (gen_random_uuid(), 'REPORTING',      'Báo cáo',   'Thống kê và xuất báo cáo',                         'chart-column', 60, TRUE, CURRENT_TIMESTAMP),
  (gen_random_uuid(), 'SYSTEM',         'Hệ thống',  'Vai trò, quyền và nhật ký hệ thống',               'settings',     70, TRUE, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  icon = EXCLUDED.icon,
  sort_order = EXCLUDED.sort_order,
  active = EXCLUDED.active;

-- -------------------------------------------------------------
-- 2. PERMISSION GROUPS - code khop link sidebar admin
-- -------------------------------------------------------------
WITH group_seed(module_code, code, name, description) AS (
  VALUES
    ('OVERVIEW',       'DASHBOARD',      'Trang chủ',             'Dashboard quản trị'),
    ('CATALOG',        'BRAND',          'Quản lý brand',         'Quản lý thương hiệu'),
    ('CATALOG',        'COLLECTION',     'Quản lý bộ sưu tập',    'Quản lý bộ sưu tập sản phẩm'),
    ('CATALOG',        'CATEGORY',       'Quản lý danh mục',      'Quản lý danh mục sản phẩm'),
    ('CATALOG',        'PRODUCT',        'Quản lý sản phẩm',      'Quản lý sản phẩm và biến thể'),
    ('CATALOG',        'INVENTORY',      'Quản lý kho',           'Tra cứu và điều chỉnh tồn kho'),
    ('CATALOG',        'TAG',            'Quản lý tag',           'Quản lý nhãn sản phẩm'),
    ('SALES',          'ORDER',          'Đơn hàng',              'Quản lý đơn hàng'),
    ('SALES',          'CUSTOMER',       'Khách hàng',            'Quản lý khách hàng và hạng thành viên'),
    ('SALES',          'PROMOTION',      'Khuyến mãi',            'Quản lý chương trình khuyến mãi'),
    ('HUMAN_RESOURCE', 'EMPLOYEE',       'Nhân viên',             'Quản lý tài khoản và hồ sơ nhân viên'),
    ('HUMAN_RESOURCE', 'DEPARTMENT',     'Phòng ban',             'Quản lý cơ cấu phòng ban của doanh nghiệp'),
    ('HUMAN_RESOURCE', 'POSITION',       'Vị trí',                'Quản lý danh mục vị trí theo phòng ban'),
    ('HUMAN_RESOURCE', 'TASK',           'Công việc & hiệu suất', 'Quản lý công việc và báo cáo hiệu suất'),
    ('WAREHOUSE',      'IMPORT_RECEIPT', 'Phiếu nhập',            'Quản lý phiếu nhập kho'),
    ('WAREHOUSE',      'EXPORT_RECEIPT', 'Phiếu xuất',            'Quản lý phiếu xuất kho'),
    ('WAREHOUSE',      'SUPPLIER',       'Nhà cung cấp',          'Quản lý nhà cung cấp'),
    ('REPORTING',      'STATISTICS',     'Thống kê',              'Xem và xuất báo cáo thống kê'),
    ('SYSTEM',         'ROLE',           'Phân quyền',            'Quản lý role và phân quyền'),
    ('SYSTEM',         'SETTINGS',       'Cấu hình hệ thống',     'Quản lý module, nhóm quyền và permission'),
    ('SYSTEM',         'LOG',            'Log hệ thống',          'Tra cứu nhật ký hệ thống')
)
INSERT INTO permission_groups (id, module_id, code, name, description, created_at)
SELECT gen_random_uuid(), m.id, s.code, s.name, s.description, CURRENT_TIMESTAMP
FROM group_seed s
JOIN modules m ON m.code = s.module_code
ON CONFLICT (code) DO UPDATE SET
  module_id = EXCLUDED.module_id,
  name = EXCLUDED.name,
  description = EXCLUDED.description;

-- -------------------------------------------------------------
-- 3. PERMISSIONS - action/button va authority backend
-- -------------------------------------------------------------
WITH permission_seed(group_code, code, name, description) AS (
  VALUES
    ('DASHBOARD', 'DASHBOARD_VIEW', 'Xem dashboard', 'Xem dashboard và các chỉ số tổng quan'),

    ('BRAND', 'BRAND_VIEW', 'Xem thương hiệu', 'Xem danh sách và chi tiết thương hiệu'),
    ('BRAND', 'BRAND_CREATE', 'Tạo thương hiệu', 'Tạo thương hiệu mới'),
    ('BRAND', 'BRAND_UPDATE', 'Sửa thương hiệu', 'Cập nhật thương hiệu'),
    ('BRAND', 'BRAND_DELETE', 'Xóa thương hiệu', 'Xóa thương hiệu'),

    ('COLLECTION', 'COLLECTION_VIEW', 'Xem bộ sưu tập', 'Xem danh sách và chi tiết bộ sưu tập'),
    ('COLLECTION', 'COLLECTION_CREATE', 'Tạo bộ sưu tập', 'Tạo bộ sưu tập mới'),
    ('COLLECTION', 'COLLECTION_UPDATE', 'Sửa bộ sưu tập', 'Cập nhật bộ sưu tập'),
    ('COLLECTION', 'COLLECTION_DELETE', 'Xóa bộ sưu tập', 'Xóa bộ sưu tập'),

    ('CATEGORY', 'CATEGORY_VIEW', 'Xem danh mục', 'Xem danh sách và chi tiết danh mục'),
    ('CATEGORY', 'CATEGORY_CREATE', 'Tạo danh mục', 'Tạo danh mục mới'),
    ('CATEGORY', 'CATEGORY_UPDATE', 'Sửa danh mục', 'Cập nhật danh mục'),
    ('CATEGORY', 'CATEGORY_DELETE', 'Xóa danh mục', 'Xóa danh mục'),

    ('PRODUCT', 'PRODUCT_VIEW', 'Xem sản phẩm', 'Xem danh sách và chi tiết sản phẩm'),
    ('PRODUCT', 'PRODUCT_CREATE', 'Tạo sản phẩm', 'Tạo sản phẩm mới'),
    ('PRODUCT', 'PRODUCT_UPDATE', 'Sửa sản phẩm', 'Cập nhật sản phẩm'),
    ('PRODUCT', 'PRODUCT_DELETE', 'Xóa sản phẩm', 'Xóa sản phẩm'),
    ('PRODUCT', 'PRODUCT_VARIANT_VIEW', 'Xem biến thể', 'Xem biến thể sản phẩm'),
    ('PRODUCT', 'PRODUCT_VARIANT_CREATE', 'Tạo biến thể', 'Tạo biến thể sản phẩm'),
    ('PRODUCT', 'PRODUCT_VARIANT_UPDATE', 'Sửa biến thể', 'Cập nhật biến thể sản phẩm'),
    ('PRODUCT', 'PRODUCT_VARIANT_DELETE', 'Xóa biến thể', 'Xóa biến thể sản phẩm'),

    ('INVENTORY', 'INVENTORY_VIEW', 'Xem tồn kho', 'Xem số lượng và lịch sử tồn kho'),
    ('INVENTORY', 'INVENTORY_ADJUST', 'Điều chỉnh tồn kho', 'Thực hiện điều chỉnh tồn kho'),

    ('TAG', 'TAG_VIEW', 'Xem tag', 'Xem danh sách và chi tiết tag'),
    ('TAG', 'TAG_CREATE', 'Tạo tag', 'Tạo tag mới'),
    ('TAG', 'TAG_UPDATE', 'Sửa tag', 'Cập nhật tag'),
    ('TAG', 'TAG_DELETE', 'Xóa tag', 'Xóa tag'),

    ('ORDER', 'ORDER_VIEW', 'Xem đơn hàng', 'Xem danh sách và chi tiết đơn hàng'),
    ('ORDER', 'ORDER_CREATE', 'Tạo đơn hàng', 'Tạo đơn hàng mới'),
    ('ORDER', 'ORDER_UPDATE', 'Sửa đơn hàng', 'Cập nhật đơn hàng'),
    ('ORDER', 'ORDER_DELETE', 'Xóa đơn hàng', 'Xóa đơn hàng'),
    ('ORDER', 'ORDER_CONFIRM', 'Xác nhận đơn hàng', 'Xác nhận xử lý đơn hàng'),
    ('ORDER', 'ORDER_CANCEL', 'Hủy đơn hàng', 'Hủy đơn hàng'),

    ('CUSTOMER', 'CUSTOMER_VIEW', 'Xem khách hàng', 'Xem danh sách và chi tiết khách hàng'),
    ('CUSTOMER', 'CUSTOMER_CREATE', 'Tạo khách hàng', 'Tạo khách hàng mới'),
    ('CUSTOMER', 'CUSTOMER_UPDATE', 'Sửa khách hàng', 'Cập nhật khách hàng'),
    ('CUSTOMER', 'CUSTOMER_DELETE', 'Xóa khách hàng', 'Xóa khách hàng'),
    ('CUSTOMER', 'CUSTOMER_STATUS_MANAGE', 'Quản lý trạng thái khách hàng', 'Kích hoạt, vô hiệu hóa, khóa hoặc mở khóa khách hàng'),
    ('CUSTOMER', 'CUSTOMER_TIER_MANAGE', 'Quản lý hạng khách hàng', 'Gán và thay đổi hạng thành viên'),

    ('PROMOTION', 'PROMOTION_VIEW', 'Xem khuyến mãi', 'Xem danh sách và chi tiết khuyến mãi'),
    ('PROMOTION', 'PROMOTION_CREATE', 'Tạo khuyến mãi', 'Tạo khuyến mãi mới'),
    ('PROMOTION', 'PROMOTION_UPDATE', 'Sửa khuyến mãi', 'Cập nhật khuyến mãi'),
    ('PROMOTION', 'PROMOTION_DELETE', 'Xóa khuyến mãi', 'Xóa khuyến mãi'),

    ('EMPLOYEE', 'USER_VIEW', 'Xem nhân viên', 'Xem danh sách và hồ sơ nhân viên'),
    ('EMPLOYEE', 'USER_CREATE', 'Tạo nhân viên', 'Tạo tài khoản nhân viên; authority đang được AuthController sử dụng'),
    ('EMPLOYEE', 'USER_UPDATE', 'Sửa nhân viên', 'Cập nhật tài khoản và hồ sơ nhân viên'),
    ('EMPLOYEE', 'USER_DELETE', 'Xóa nhân viên', 'Xóa hoặc vô hiệu hóa nhân viên'),
    ('EMPLOYEE', 'USER_CREATE_ADMIN', 'Tạo quản trị viên', 'Tạo tài khoản admin; authority đang được AuthController sử dụng'),

    ('DEPARTMENT', 'DEPARTMENT_VIEW', 'Xem phòng ban', 'Xem danh sách và chi tiết phòng ban'),
    ('DEPARTMENT', 'DEPARTMENT_CREATE', 'Tạo phòng ban', 'Tạo phòng ban mới'),
    ('DEPARTMENT', 'DEPARTMENT_UPDATE', 'Sửa phòng ban', 'Cập nhật phòng ban'),
    ('DEPARTMENT', 'DEPARTMENT_DELETE', 'Xóa phòng ban', 'Xóa phòng ban chưa được sử dụng'),

    ('POSITION', 'POSITION_VIEW', 'Xem vị trí', 'Xem danh sách và chi tiết vị trí'),
    ('POSITION', 'POSITION_CREATE', 'Tạo vị trí', 'Tạo vị trí mới trong phòng ban'),
    ('POSITION', 'POSITION_UPDATE', 'Sửa vị trí', 'Cập nhật vị trí và phòng ban trực thuộc'),
    ('POSITION', 'POSITION_DELETE', 'Xóa vị trí', 'Xóa vị trí chưa được nhân viên sử dụng'),

    ('TASK', 'TASK_VIEW', 'Xem công việc', 'Xem danh sách và chi tiết công việc'),
    ('TASK', 'TASK_CREATE', 'Tạo công việc', 'Tạo công việc mới'),
    ('TASK', 'TASK_UPDATE', 'Sửa công việc', 'Cập nhật công việc'),
    ('TASK', 'TASK_DELETE', 'Xóa công việc', 'Xóa công việc'),
    ('TASK', 'TASK_ASSIGN', 'Giao việc', 'Giao công việc cho nhân viên'),
    ('TASK', 'TASK_REPORT', 'Báo cáo công việc', 'Gửi hoặc duyệt báo cáo công việc'),

    ('IMPORT_RECEIPT', 'IMPORT_RECEIPT_VIEW', 'Xem phiếu nhập', 'Xem danh sách và chi tiết phiếu nhập'),
    ('IMPORT_RECEIPT', 'IMPORT_RECEIPT_CREATE', 'Tạo phiếu nhập', 'Tạo phiếu nhập kho'),
    ('IMPORT_RECEIPT', 'IMPORT_RECEIPT_UPDATE', 'Sửa phiếu nhập', 'Cập nhật phiếu nhập kho'),
    ('IMPORT_RECEIPT', 'IMPORT_RECEIPT_DELETE', 'Xóa phiếu nhập', 'Xóa phiếu nhập kho'),
    ('IMPORT_RECEIPT', 'IMPORT_RECEIPT_APPROVE', 'Duyệt phiếu nhập', 'Duyệt phiếu nhập và cập nhật kho'),
    ('IMPORT_RECEIPT', 'IMPORT_RECEIPT_CANCEL', 'Hủy phiếu nhập', 'Hủy phiếu nhập kho'),

    ('EXPORT_RECEIPT', 'EXPORT_RECEIPT_VIEW', 'Xem phiếu xuất', 'Xem danh sách và chi tiết phiếu xuất'),
    ('EXPORT_RECEIPT', 'EXPORT_RECEIPT_CREATE', 'Tạo phiếu xuất', 'Tạo phiếu xuất kho'),
    ('EXPORT_RECEIPT', 'EXPORT_RECEIPT_UPDATE', 'Sửa phiếu xuất', 'Cập nhật phiếu xuất kho'),
    ('EXPORT_RECEIPT', 'EXPORT_RECEIPT_DELETE', 'Xóa phiếu xuất', 'Xóa phiếu xuất kho'),
    ('EXPORT_RECEIPT', 'EXPORT_RECEIPT_APPROVE', 'Duyệt phiếu xuất', 'Duyệt phiếu xuất và cập nhật kho'),
    ('EXPORT_RECEIPT', 'EXPORT_RECEIPT_CANCEL', 'Hủy phiếu xuất', 'Hủy phiếu xuất kho'),

    ('SUPPLIER', 'SUPPLIER_VIEW', 'Xem nhà cung cấp', 'Xem danh sách và chi tiết nhà cung cấp'),
    ('SUPPLIER', 'SUPPLIER_CREATE', 'Tạo nhà cung cấp', 'Tạo nhà cung cấp mới'),
    ('SUPPLIER', 'SUPPLIER_UPDATE', 'Sửa nhà cung cấp', 'Cập nhật nhà cung cấp'),
    ('SUPPLIER', 'SUPPLIER_DELETE', 'Xóa nhà cung cấp', 'Xóa nhà cung cấp'),

    ('STATISTICS', 'STATISTICS_VIEW', 'Xem thống kê', 'Xem báo cáo và biểu đồ thống kê'),
    ('STATISTICS', 'STATISTICS_EXPORT', 'Xuất báo cáo', 'Xuất dữ liệu thống kê'),

    ('ROLE', 'ROLE_VIEW', 'Xem phân quyền', 'Xem role, catalog quyền và người được gán role'),
    ('ROLE', 'ROLE_CREATE', 'Tạo role', 'Tạo role mới'),
    ('ROLE', 'ROLE_UPDATE', 'Sửa role', 'Cập nhật role và danh sách permission'),
    ('ROLE', 'ROLE_DELETE', 'Xóa role', 'Xóa role'),
    ('ROLE', 'ROLE_ASSIGN', 'Gán role', 'Gán role hoặc quyền trực tiếp cho người dùng'),
    ('ROLE', 'AUTHORIZATION_MANAGE', 'Quản trị phân quyền', 'Authority bảo vệ toàn bộ API /api/admin/authorization'),

    ('SETTINGS', 'SETTINGS_MANAGE', 'Quản lý cấu hình quyền', 'Quản lý catalog module, permission group và permission'),

    ('LOG', 'LOG_VIEW', 'Xem log hệ thống', 'Tra cứu nhật ký và lịch sử hoạt động')
)
INSERT INTO permissions (id, group_id, code, name, description, created_at)
SELECT gen_random_uuid(), pg.id, s.code, s.name, s.description, CURRENT_TIMESTAMP
FROM permission_seed s
JOIN permission_groups pg ON pg.code = s.group_code
ON CONFLICT (code) DO UPDATE SET
  group_id = EXCLUDED.group_id,
  name = EXCLUDED.name,
  description = EXCLUDED.description;

-- -------------------------------------------------------------
-- 4. ROLES
-- Role code khong co tien to ROLE_. Backend tu them ROLE_ khi tao JWT.
-- -------------------------------------------------------------
INSERT INTO roles (id, code, name, description, created_at)
VALUES
  (gen_random_uuid(), 'SUPER_ADMIN', 'Quản trị viên tối cao', 'Toàn quyền hệ thống, bao gồm quản trị role và tạo admin', CURRENT_TIMESTAMP),
  (gen_random_uuid(), 'ADMIN',       'Quản trị viên',         'Quản trị toàn bộ nghiệp vụ, không được quản trị phân quyền tối cao', CURRENT_TIMESTAMP),
  (gen_random_uuid(), 'MANAGER',     'Quản lý',               'Quản lý catalog, bán hàng, nhân sự và xem báo cáo', CURRENT_TIMESTAMP),
  (gen_random_uuid(), 'STAFF',       'Nhân viên bán hàng',    'Xử lý đơn hàng, khách hàng và tra cứu sản phẩm', CURRENT_TIMESTAMP),
  (gen_random_uuid(), 'WAREHOUSE',   'Nhân viên kho',         'Quản lý tồn kho, phiếu nhập, phiếu xuất và nhà cung cấp', CURRENT_TIMESTAMP),
  (gen_random_uuid(), 'ACCOUNTANT',  'Kế toán',               'Tra cứu chứng từ, đơn hàng và báo cáo', CURRENT_TIMESTAMP)
ON CONFLICT (code) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description;

-- -------------------------------------------------------------
-- 5. ROLE PERMISSIONS
-- Tat ca grant mac dinh scope ALL. Khi can gioi han theo nhan vien,
-- team hoac phong ban, doi thanh SELF/TEAM/DEPARTMENT qua API quan tri.
-- -------------------------------------------------------------
WITH role_permission_seed(role_code, permission_code) AS (
  -- SUPER_ADMIN nhan moi permission hien co, ke ca permission custom.
  SELECT 'SUPER_ADMIN', p.code
  FROM permissions p

  UNION ALL

  -- ADMIN quan tri nghiep vu nhung khong duoc tao admin/quan tri RBAC.
  SELECT 'ADMIN', p.code
  FROM permissions p
  JOIN permission_groups pg ON pg.id = p.group_id
  WHERE pg.code <> 'ROLE'
    AND p.code <> 'USER_CREATE_ADMIN'

  UNION ALL

  -- MANAGER: day du nghiep vu ban hang/catalog/nhan su; kho chi duoc xem.
  SELECT 'MANAGER', p.code
  FROM permissions p
  JOIN permission_groups pg ON pg.id = p.group_id
  WHERE pg.code IN (
    'DASHBOARD', 'BRAND', 'COLLECTION', 'CATEGORY', 'PRODUCT', 'TAG',
    'ORDER', 'CUSTOMER', 'PROMOTION', 'EMPLOYEE', 'DEPARTMENT', 'POSITION', 'TASK', 'STATISTICS'
  )
    AND p.code NOT IN ('USER_CREATE_ADMIN', 'USER_DELETE')

  UNION ALL

  SELECT 'MANAGER', p.code
  FROM permissions p
  WHERE p.code IN ('INVENTORY_VIEW', 'IMPORT_RECEIPT_VIEW', 'EXPORT_RECEIPT_VIEW', 'SUPPLIER_VIEW')

  UNION ALL

  -- STAFF: tra cuu catalog, xu ly don/khach hang va tu bao cao cong viec.
  SELECT 'STAFF', p.code
  FROM permissions p
  WHERE p.code IN (
    'DASHBOARD_VIEW',
    'BRAND_VIEW', 'COLLECTION_VIEW', 'CATEGORY_VIEW', 'PRODUCT_VIEW',
    'PRODUCT_VARIANT_VIEW', 'INVENTORY_VIEW', 'TAG_VIEW',
    'ORDER_VIEW', 'ORDER_CREATE', 'ORDER_UPDATE', 'ORDER_CONFIRM', 'ORDER_CANCEL',
    'CUSTOMER_VIEW', 'CUSTOMER_CREATE', 'CUSTOMER_UPDATE',
    'PROMOTION_VIEW', 'TASK_VIEW', 'TASK_REPORT'
  )

  UNION ALL

  -- WAREHOUSE: catalog can thiet va toan bo nghiep vu kho van.
  SELECT 'WAREHOUSE', p.code
  FROM permissions p
  JOIN permission_groups pg ON pg.id = p.group_id
  WHERE pg.code IN ('INVENTORY', 'IMPORT_RECEIPT', 'EXPORT_RECEIPT', 'SUPPLIER')

  UNION ALL

  SELECT 'WAREHOUSE', p.code
  FROM permissions p
  WHERE p.code IN ('DASHBOARD_VIEW', 'PRODUCT_VIEW', 'PRODUCT_VARIANT_VIEW', 'ORDER_VIEW')

  UNION ALL

  -- ACCOUNTANT: quyen doc chung tu va xuat bao cao.
  SELECT 'ACCOUNTANT', p.code
  FROM permissions p
  WHERE p.code IN (
    'DASHBOARD_VIEW', 'ORDER_VIEW', 'CUSTOMER_VIEW', 'PROMOTION_VIEW',
    'IMPORT_RECEIPT_VIEW', 'EXPORT_RECEIPT_VIEW', 'SUPPLIER_VIEW',
    'STATISTICS_VIEW', 'STATISTICS_EXPORT'
  )
)
INSERT INTO role_permissions (role_id, permission_id, scope)
SELECT DISTINCT r.id, p.id,
  CASE
    WHEN r.code = 'MANAGER'
      AND p.code IN ('USER_VIEW', 'USER_CREATE', 'USER_UPDATE', 'USER_DELETE')
      THEN 'STORE'
    ELSE 'ALL'
  END
FROM role_permission_seed s
JOIN roles r ON r.code = s.role_code
JOIN permissions p ON p.code = s.permission_code
ON CONFLICT (role_id, permission_id) DO UPDATE SET
  scope = EXCLUDED.scope;

-- -------------------------------------------------------------
-- 6. TAI KHOAN SUPER ADMIN DAU TIEN
-- Username: superadmin
-- Email:    superadmin@lunaria.local
-- Mat khau tam thoi duoc sinh ngau nhien va in bang PostgreSQL NOTICE
-- DUY NHAT o lan tao dau tien. Hay luu lai va doi ngay sau khi dang nhap.
-- Neu username da ton tai, script chi dam bao role SUPER_ADMIN duoc gan,
-- hoan toan khong ghi de mat khau hoac thong tin tai khoan.
-- -------------------------------------------------------------
DO $bootstrap_super_admin$
DECLARE
  v_user_id UUID;
  v_temporary_password TEXT;
BEGIN
  SELECT id INTO v_user_id
  FROM users
  WHERE username = 'superadmin';

  IF v_user_id IS NULL THEN
    IF EXISTS (
      SELECT 1 FROM users WHERE email = 'superadmin@lunaria.local'
    ) THEN
      RAISE EXCEPTION
        'Email superadmin@lunaria.local da thuoc mot username khac; khong the tao superadmin an toan';
    END IF;

    v_temporary_password := encode(gen_random_bytes(18), 'hex');

    INSERT INTO users (
      id,
      username,
      full_name,
      email,
      password_hash,
      job_title,
      employment_type,
      employment_status,
      active,
      locked,
      failed_login_attempts,
      last_password_change,
      email_verified,
      phone_verified,
      created_at
    )
    VALUES (
      gen_random_uuid(),
      'superadmin',
      'Quản trị viên tối cao',
      'superadmin@lunaria.local',
      crypt(v_temporary_password, gen_salt('bf', 12)),
      'Quản trị hệ thống',
      'FULL_TIME',
      'ACTIVE',
      TRUE,
      FALSE,
      0,
      CURRENT_TIMESTAMP,
      TRUE,
      FALSE,
      CURRENT_TIMESTAMP
    )
    RETURNING id INTO v_user_id;

    RAISE NOTICE '=============================================================';
    RAISE NOTICE 'Da tao SUPER ADMIN dau tien';
    RAISE NOTICE 'Username: superadmin';
    RAISE NOTICE 'Mat khau tam thoi: %', v_temporary_password;
    RAISE NOTICE 'Hay doi mat khau ngay sau lan dang nhap dau tien.';
    RAISE NOTICE '=============================================================';
  ELSE
    RAISE NOTICE 'Username superadmin da ton tai; giu nguyen thong tin va mat khau.';
  END IF;

  INSERT INTO user_roles (user_id, role_id, assigned_at)
  SELECT v_user_id, r.id, CURRENT_TIMESTAMP
  FROM roles r
  WHERE r.code = 'SUPER_ADMIN'
  ON CONFLICT (user_id, role_id) DO NOTHING;
END;
$bootstrap_super_admin$;

COMMIT;

-- -------------------------------------------------------------
-- Truy van kiem tra sau khi chay
-- -------------------------------------------------------------
-- SELECT m.code, pg.code, p.code
-- FROM modules m
-- JOIN permission_groups pg ON pg.module_id = m.id
-- JOIN permissions p ON p.group_id = pg.id
-- ORDER BY m.sort_order, pg.code, p.code;
--
-- SELECT r.code, COUNT(rp.permission_id) AS permission_count
-- FROM roles r
-- LEFT JOIN role_permissions rp ON rp.role_id = r.id
-- GROUP BY r.code
-- ORDER BY r.code;
--
-- SELECT u.username, r.code
-- FROM users u
-- JOIN user_roles ur ON ur.user_id = u.id
-- JOIN roles r ON r.id = ur.role_id
-- WHERE u.username = 'superadmin';
