-- =============================================================
-- DU LIEU GIA: 20 NHAN VIEN NOI BO
-- PostgreSQL / Fashion System
--
-- Tai khoan demo:
--   Mat khau chung: NhanVien@123
--   Ma nhan vien:   NV-2609-0001 ... NV-2609-0020
--
-- An toan khi chay:
--   - Khong UPDATE hoac DELETE bat ky nhan vien hien co nao.
--   - Chay lai khong tao trung nhan vien, role, phong ban hay cua hang.
--   - Neu username/email/phone/ma nhan vien da thuoc du lieu khac,
--     ban ghi demo do duoc bo qua va du lieu cu duoc giu nguyen.
--   - Phong ban/vi tri/cua hang duoc lay tu du lieu dang co; neu chua co
--     thi quan he tuong ung duoc bo qua.
--
-- Chay tu thu muc backend/fashion-system:
--   psql -v ON_ERROR_STOP=1 -d commerce_db \
--     -f src/main/java/com/fashionsystem/fashion_system/db/seed_employee_demo.sql
-- =============================================================

BEGIN;

CREATE TEMP TABLE employee_demo_seed (
  seed_order    INTEGER PRIMARY KEY,
  id            UUID NOT NULL UNIQUE,
  username      VARCHAR(50) NOT NULL UNIQUE,
  employee_code VARCHAR(30) NOT NULL UNIQUE,
  full_name     VARCHAR(255) NOT NULL,
  email         VARCHAR(255) NOT NULL UNIQUE,
  phone         VARCHAR(20) NOT NULL UNIQUE,
  date_of_birth DATE,
  gender        VARCHAR(20),
  role_code     VARCHAR(50) NOT NULL,
  position_slot INTEGER NOT NULL,
  store_slot    INTEGER NOT NULL,
  employment_type VARCHAR(30) NOT NULL,
  hire_date     DATE NOT NULL
) ON COMMIT DROP;

INSERT INTO employee_demo_seed (
  seed_order, id, username, employee_code, full_name, email, phone,
  date_of_birth, gender, role_code, position_slot, store_slot,
  employment_type, hire_date
)
VALUES
  ( 1, '71000000-0000-0000-0000-000000000001', 'minh.nguyen',  'NV-2609-0001', 'Nguyễn Hoàng Minh',  'minh.nguyen@demo.lunaria.vn',  '0909000001', DATE '1990-03-12', 'MALE',   'MANAGER',    1, 1, 'FULL_TIME', DATE '2021-02-15'),
  ( 2, '71000000-0000-0000-0000-000000000002', 'linh.tran',    'NV-2609-0002', 'Trần Mỹ Linh',       'linh.tran@demo.lunaria.vn',    '0909000002', DATE '1992-08-24', 'FEMALE', 'MANAGER',    2, 2, 'FULL_TIME', DATE '2021-06-01'),
  ( 3, '71000000-0000-0000-0000-000000000003', 'khoa.le',      'NV-2609-0003', 'Lê Anh Khoa',        'khoa.le@demo.lunaria.vn',      '0909000003', DATE '1989-11-08', 'MALE',   'MANAGER',    3, 3, 'FULL_TIME', DATE '2020-09-10'),
  ( 4, '71000000-0000-0000-0000-000000000004', 'thao.pham',    'NV-2609-0004', 'Phạm Thu Thảo',      'thao.pham@demo.lunaria.vn',    '0909000004', DATE '1991-05-17', 'FEMALE', 'MANAGER',    4, 1, 'FULL_TIME', DATE '2022-01-05'),
  ( 5, '71000000-0000-0000-0000-000000000005', 'an.nguyen',    'NV-2609-0005', 'Nguyễn Gia An',      'an.nguyen@demo.lunaria.vn',    '0909000005', DATE '1998-01-21', 'MALE',   'STAFF',      5, 1, 'FULL_TIME', DATE '2024-03-11'),
  ( 6, '71000000-0000-0000-0000-000000000006', 'vy.tran',      'NV-2609-0006', 'Trần Ngọc Vy',       'vy.tran@demo.lunaria.vn',      '0909000006', DATE '2000-04-09', 'FEMALE', 'STAFF',      6, 1, 'PART_TIME', DATE '2025-01-06'),
  ( 7, '71000000-0000-0000-0000-000000000007', 'huy.le',       'NV-2609-0007', 'Lê Quốc Huy',        'huy.le@demo.lunaria.vn',       '0909000007', DATE '1997-07-16', 'MALE',   'STAFF',      7, 2, 'FULL_TIME', DATE '2023-08-14'),
  ( 8, '71000000-0000-0000-0000-000000000008', 'mai.pham',     'NV-2609-0008', 'Phạm Thanh Mai',     'mai.pham@demo.lunaria.vn',     '0909000008', DATE '1999-12-03', 'FEMALE', 'STAFF',      8, 2, 'FULL_TIME', DATE '2024-05-20'),
  ( 9, '71000000-0000-0000-0000-000000000009', 'nam.vo',       'NV-2609-0009', 'Võ Thành Nam',       'nam.vo@demo.lunaria.vn',       '0909000009', DATE '1996-02-28', 'MALE',   'WAREHOUSE',  9, 1, 'FULL_TIME', DATE '2022-10-03'),
  (10, '71000000-0000-0000-0000-000000000010', 'uyen.dang',    'NV-2609-0010', 'Đặng Bảo Uyên',      'uyen.dang@demo.lunaria.vn',    '0909000010', DATE '1998-06-19', 'FEMALE', 'WAREHOUSE', 10, 2, 'FULL_TIME', DATE '2023-02-13'),
  (11, '71000000-0000-0000-0000-000000000011', 'phuc.bui',     'NV-2609-0011', 'Bùi Minh Phúc',      'phuc.bui@demo.lunaria.vn',     '0909000011', DATE '1995-09-07', 'MALE',   'WAREHOUSE', 11, 3, 'FULL_TIME', DATE '2022-07-18'),
  (12, '71000000-0000-0000-0000-000000000012', 'nhi.hoang',    'NV-2609-0012', 'Hoàng Yến Nhi',      'nhi.hoang@demo.lunaria.vn',    '0909000012', DATE '2001-10-25', 'FEMALE', 'STAFF',     12, 3, 'PART_TIME', DATE '2025-04-07'),
  (13, '71000000-0000-0000-0000-000000000013', 'duc.do',       'NV-2609-0013', 'Đỗ Anh Đức',         'duc.do@demo.lunaria.vn',       '0909000013', DATE '1994-03-30', 'MALE',   'ACCOUNTANT', 13, 1, 'FULL_TIME', DATE '2021-11-22'),
  (14, '71000000-0000-0000-0000-000000000014', 'ha.ngo',       'NV-2609-0014', 'Ngô Thu Hà',         'ha.ngo@demo.lunaria.vn',       '0909000014', DATE '1993-08-11', 'FEMALE', 'ACCOUNTANT', 14, 2, 'FULL_TIME', DATE '2022-04-04'),
  (15, '71000000-0000-0000-0000-000000000015', 'tuan.dinh',    'NV-2609-0015', 'Đinh Minh Tuấn',     'tuan.dinh@demo.lunaria.vn',    '0909000015', DATE '1997-01-14', 'MALE',   'STAFF',     15, 3, 'FULL_TIME', DATE '2023-09-25'),
  (16, '71000000-0000-0000-0000-000000000016', 'tram.ly',      'NV-2609-0016', 'Lý Ngọc Trâm',       'tram.ly@demo.lunaria.vn',      '0909000016', DATE '2000-05-06', 'FEMALE', 'STAFF',     16, 1, 'INTERN',    DATE '2026-06-01'),
  (17, '71000000-0000-0000-0000-000000000017', 'son.huynh',    'NV-2609-0017', 'Huỳnh Thanh Sơn',    'son.huynh@demo.lunaria.vn',    '0909000017', DATE '1996-11-22', 'MALE',   'WAREHOUSE', 17, 2, 'FULL_TIME', DATE '2023-01-09'),
  (18, '71000000-0000-0000-0000-000000000018', 'quynh.duong',  'NV-2609-0018', 'Dương Như Quỳnh',    'quynh.duong@demo.lunaria.vn',  '0909000018', DATE '1999-04-18', 'FEMALE', 'STAFF',     18, 2, 'FULL_TIME', DATE '2024-02-19'),
  (19, '71000000-0000-0000-0000-000000000019', 'long.truong',  'NV-2609-0019', 'Trương Hoàng Long',  'long.truong@demo.lunaria.vn',  '0909000019', DATE '1995-07-02', 'MALE',   'STAFF',     19, 3, 'CONTRACT',  DATE '2025-07-01'),
  (20, '71000000-0000-0000-0000-000000000020', 'chau.lam',     'NV-2609-0020', 'Lâm Minh Châu',      'chau.lam@demo.lunaria.vn',     '0909000020', DATE '2001-09-15', 'OTHER',  'STAFF',     20, 1, 'FULL_TIME', DATE '2025-10-13');

-- Bao dam cac role nghiep vu toi thieu da duoc seed truoc.
DO $check_required_roles$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM (VALUES ('MANAGER'), ('STAFF'), ('WAREHOUSE'), ('ACCOUNTANT')) required(code)
    WHERE NOT EXISTS (SELECT 1 FROM roles r WHERE r.code = required.code)
  ) THEN
    RAISE EXCEPTION
      'Thieu role bat buoc. Hay chay seed_authorization.sql truoc seed_employee_demo.sql.';
  END IF;
END
$check_required_roles$;

-- Gan vi tri theo thu tu cac vi tri dang hoat dong. Neu chua co vi tri,
-- position_id va job_title van de trong de khong tao du lieu danh muc ngoai y muon.
WITH ranked_positions AS (
  SELECT p.id, p.name,
         ROW_NUMBER() OVER (ORDER BY p.department_id, p.hierarchy_level DESC, p.code) AS rn,
         COUNT(*) OVER () AS total
  FROM positions p
  JOIN departments d ON d.id = p.department_id
  WHERE p.active = TRUE AND d.active = TRUE
), prepared AS (
  SELECT s.*,
         p.id AS position_id,
         p.name AS position_name
  FROM employee_demo_seed s
  LEFT JOIN ranked_positions p
    ON p.rn = MOD(s.position_slot - 1, p.total) + 1
)
INSERT INTO users (
  id, username, employee_code, full_name, email, phone, password_hash,
  date_of_birth, gender, job_title, position_id, employment_type,
  employment_status, hire_date, active, locked, failed_login_attempts,
  last_password_change, email_verified, phone_verified, created_at
)
SELECT
  p.id, p.username, p.employee_code, p.full_name, p.email, p.phone,
  crypt('NhanVien@123', gen_salt('bf', 10)),
  p.date_of_birth, p.gender, p.position_name, p.position_id,
  p.employment_type, 'ACTIVE', p.hire_date,
  TRUE, FALSE, 0, CURRENT_TIMESTAMP, FALSE, FALSE, CURRENT_TIMESTAMP
FROM prepared p
ON CONFLICT DO NOTHING;

-- Chi gan role cho dung ban ghi demo (khop ca UUID, username va ma nhan vien).
INSERT INTO user_roles (user_id, role_id, assigned_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP
FROM employee_demo_seed s
JOIN users u
  ON u.id = s.id
 AND u.username = s.username
 AND u.employee_code = s.employee_code
JOIN roles r ON r.code = s.role_code
ON CONFLICT (user_id, role_id) DO NOTHING;

-- Phong ban duoc suy ra tu vi tri da gan.
INSERT INTO user_departments (user_id, department_id, assigned_at)
SELECT u.id, p.department_id, CURRENT_TIMESTAMP
FROM employee_demo_seed s
JOIN users u
  ON u.id = s.id
 AND u.username = s.username
 AND u.employee_code = s.employee_code
JOIN positions p ON p.id = u.position_id
ON CONFLICT (user_id, department_id) DO NOTHING;

-- Moi nhan vien chi duoc them vao mot cua hang dang hoat dong neu database co cua hang.
WITH ranked_stores AS (
  SELECT st.id,
         ROW_NUMBER() OVER (ORDER BY st.code) AS rn,
         COUNT(*) OVER () AS total
  FROM stores st
  WHERE st.active = TRUE
), selected_stores AS (
  SELECT s.id AS user_id, s.username, s.employee_code, s.role_code, st.id AS store_id
  FROM employee_demo_seed s
  LEFT JOIN ranked_stores st
    ON st.rn = MOD(s.store_slot - 1, st.total) + 1
)
INSERT INTO store_staffs (
  id, user_id, store_id, staff_role, start_date, active, created_at
)
SELECT
  gen_random_uuid(), u.id, ss.store_id, ss.role_code, u.hire_date, TRUE, CURRENT_TIMESTAMP
FROM selected_stores ss
JOIN users u
  ON u.id = ss.user_id
 AND u.username = ss.username
 AND u.employee_code = ss.employee_code
WHERE ss.store_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM store_staffs existing
    WHERE existing.user_id = u.id
      AND existing.store_id = ss.store_id
      AND existing.active = TRUE
  );

DO $seed_result$
DECLARE
  seeded_count INTEGER;
BEGIN
  SELECT COUNT(*)
  INTO seeded_count
  FROM employee_demo_seed s
  JOIN users u
    ON u.id = s.id
   AND u.username = s.username
   AND u.employee_code = s.employee_code;

  RAISE NOTICE 'Da co %/20 nhan vien demo. Cac ban ghi trung du lieu cu (neu co) da duoc bo qua.', seeded_count;
  RAISE NOTICE 'Mat khau chung cua tai khoan demo: NhanVien@123';
END
$seed_result$;

COMMIT;

-- Kiem tra nhanh sau khi chay:
-- SELECT u.employee_code, u.username, u.full_name, r.code AS role_code,
--        d.code AS department_code, p.code AS position_code, st.code AS store_code
-- FROM users u
-- LEFT JOIN user_roles ur ON ur.user_id = u.id
-- LEFT JOIN roles r ON r.id = ur.role_id
-- LEFT JOIN user_departments ud ON ud.user_id = u.id
-- LEFT JOIN departments d ON d.id = ud.department_id
-- LEFT JOIN positions p ON p.id = u.position_id
-- LEFT JOIN store_staffs ss ON ss.user_id = u.id AND ss.active = TRUE
-- LEFT JOIN stores st ON st.id = ss.store_id
-- WHERE u.id::text LIKE '71000000-0000-0000-0000-%'
-- ORDER BY u.employee_code;
