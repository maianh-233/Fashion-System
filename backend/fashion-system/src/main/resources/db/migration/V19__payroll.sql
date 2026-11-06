-- Tính lương & đãi ngộ: lương cố định hằng tháng (không hoa hồng), phụ cấp / thưởng / khấu trừ cấu hình được,
-- tăng ca theo giờ đã duyệt, kỳ lương theo tháng cho từng cửa hàng hoặc Văn phòng (nhân viên Global),
-- quy trình DRAFT → CALCULATED → PENDING_APPROVAL → APPROVED → PAID (hoặc CANCELLED).
-- Số tiền lưu bằng VND nguyên (numeric(15,0)); dữ liệu nhân viên / chấm công đọc từ bảng hiện có, không sao chép.

-- ========== CẤU HÌNH CHUNG ==========

CREATE TABLE payroll_settings (
    id SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    -- CALENDAR: ngày công chuẩn = số ngày làm việc trong tháng theo lịch; FIXED: số cố định (vd 26).
    standard_days_mode VARCHAR(20) NOT NULL DEFAULT 'CALENDAR' CHECK (standard_days_mode IN ('CALENDAR', 'FIXED')),
    fixed_standard_days NUMERIC(4,1) NOT NULL DEFAULT 26 CHECK (fixed_standard_days > 0 AND fixed_standard_days <= 31),
    ot_weekday_rate NUMERIC(4,2) NOT NULL DEFAULT 1.5 CHECK (ot_weekday_rate >= 1),
    ot_weekend_rate NUMERIC(4,2) NOT NULL DEFAULT 2.0 CHECK (ot_weekend_rate >= 1),
    ot_holiday_rate NUMERIC(4,2) NOT NULL DEFAULT 3.0 CHECK (ot_holiday_rate >= 1),
    rounding_unit INT NOT NULL DEFAULT 1000 CHECK (rounding_unit IN (1, 10, 100, 500, 1000)),
    rounding_mode VARCHAR(10) NOT NULL DEFAULT 'HALF_UP' CHECK (rounding_mode IN ('HALF_UP', 'DOWN', 'UP')),
    updated_by UUID REFERENCES users(id) ON DELETE SET NULL,
    updated_at TIMESTAMP
);
INSERT INTO payroll_settings(id) VALUES (1);

-- ========== KHOẢN PHỤ CẤP / THƯỞNG / KHẤU TRỪ ==========

CREATE TABLE payroll_components (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    kind VARCHAR(20) NOT NULL CHECK (kind IN ('ALLOWANCE', 'BONUS', 'DEDUCTION')),
    -- FIXED: cố định / tháng; PRORATED: theo tỷ lệ ngày công; PER_WORKED_DAY: × ngày đi làm thực tế;
    -- PERCENT_OF_BASE: % lương cơ bản hợp đồng; PER_LATE_EARLY: × số lần đi trễ / về sớm;
    -- PERFECT_ATTENDANCE: chỉ khi không trễ / sớm / thiếu chấm công / nghỉ không lương; MANUAL: nhập theo kỳ.
    calc_method VARCHAR(30) NOT NULL CHECK (calc_method IN ('FIXED', 'PRORATED', 'PER_WORKED_DAY', 'PERCENT_OF_BASE',
                                                            'PER_LATE_EARLY', 'PERFECT_ATTENDANCE', 'MANUAL')),
    default_amount NUMERIC(15,0) CHECK (default_amount IS NULL OR default_amount >= 0),
    default_rate NUMERIC(7,4) CHECK (default_rate IS NULL OR (default_rate >= 0 AND default_rate <= 100)),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    note TEXT,
    sort_order INT NOT NULL DEFAULT 100,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMP
);

INSERT INTO payroll_components(code, name, kind, calc_method, default_amount, default_rate, sort_order, note) VALUES
    ('MEAL', 'Phụ cấp ăn uống', 'ALLOWANCE', 'PER_WORKED_DAY', 30000, NULL, 10, 'Theo ngày đi làm thực tế'),
    ('TRANSPORT', 'Phụ cấp đi lại', 'ALLOWANCE', 'PRORATED', 500000, NULL, 20, 'Theo tỷ lệ ngày công'),
    ('PHONE', 'Phụ cấp điện thoại', 'ALLOWANCE', 'FIXED', 300000, NULL, 30, NULL),
    ('RESPONSIBILITY', 'Phụ cấp trách nhiệm', 'ALLOWANCE', 'FIXED', NULL, NULL, 40, 'Nhập mức theo chức vụ'),
    ('OTHER_ALLOWANCE', 'Phụ cấp khác', 'ALLOWANCE', 'MANUAL', NULL, NULL, 50, NULL),
    ('ATTENDANCE_BONUS', 'Thưởng chuyên cần', 'BONUS', 'PERFECT_ATTENDANCE', 300000, NULL, 110,
     'Không đi trễ / về sớm, không thiếu chấm công, không nghỉ không lương'),
    ('PERFORMANCE_BONUS', 'Thưởng hiệu suất', 'BONUS', 'MANUAL', NULL, NULL, 120, NULL),
    ('MANAGER_BONUS', 'Thưởng theo quyết định quản lý', 'BONUS', 'MANUAL', NULL, NULL, 130, NULL),
    ('OTHER_BONUS', 'Thưởng khác', 'BONUS', 'MANUAL', NULL, NULL, 140, NULL),
    ('LATE_PENALTY', 'Khấu trừ đi trễ / về sớm', 'DEDUCTION', 'PER_LATE_EARLY', 20000, NULL, 210, 'Mỗi lần vượt ngưỡng cho phép'),
    ('SOCIAL_INSURANCE', 'BHXH, BHYT, BHTN (phần người lao động)', 'DEDUCTION', 'PERCENT_OF_BASE', NULL, 10.5, 220,
     'Nhập tay theo doanh nghiệp; chưa tính tự động mức trần'),
    ('PERSONAL_INCOME_TAX', 'Thuế TNCN', 'DEDUCTION', 'MANUAL', NULL, NULL, 230, 'Nhập theo kỳ; chưa tính lũy tiến tự động'),
    ('OTHER_DEDUCTION', 'Khấu trừ khác', 'DEDUCTION', 'MANUAL', NULL, NULL, 240, NULL);

-- ========== HỒ SƠ LƯƠNG ==========

-- Mỗi lần đổi lương là một phiên bản mới (effective_from); phiên bản cũ chỉ được đóng effective_to, không ghi đè số tiền.
CREATE TABLE salary_structures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    base_salary NUMERIC(15,0) NOT NULL CHECK (base_salary > 0),
    effective_from DATE NOT NULL,
    effective_to DATE,
    pay_method VARCHAR(20) NOT NULL DEFAULT 'BANK_TRANSFER' CHECK (pay_method IN ('BANK_TRANSFER', 'CASH')),
    bank_name VARCHAR(100),
    bank_account VARCHAR(50),
    overtime_eligible BOOLEAN NOT NULL DEFAULT TRUE,
    overtime_hourly_rate NUMERIC(15,0) CHECK (overtime_hourly_rate IS NULL OR overtime_hourly_rate > 0),
    confidential BOOLEAN NOT NULL DEFAULT FALSE,
    note TEXT,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMP,
    CONSTRAINT chk_salary_structures_dates CHECK (effective_to IS NULL OR effective_to >= effective_from),
    CONSTRAINT uq_salary_structures_start UNIQUE (user_id, effective_from)
);
CREATE INDEX idx_salary_structures_user ON salary_structures(user_id, effective_from DESC);

CREATE TABLE salary_structure_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    structure_id UUID NOT NULL REFERENCES salary_structures(id) ON DELETE CASCADE,
    component_id UUID NOT NULL REFERENCES payroll_components(id),
    amount NUMERIC(15,0) CHECK (amount IS NULL OR amount >= 0),
    rate NUMERIC(7,4) CHECK (rate IS NULL OR (rate >= 0 AND rate <= 100)),
    note TEXT,
    CONSTRAINT uq_salary_structure_items UNIQUE (structure_id, component_id)
);

CREATE TABLE salary_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    structure_id UUID REFERENCES salary_structures(id) ON DELETE SET NULL,
    action VARCHAR(30) NOT NULL CHECK (action IN ('CREATE', 'NEW_VERSION', 'UPDATE', 'CLOSE')),
    old_data JSONB,
    new_data JSONB,
    reason TEXT NOT NULL,
    changed_by UUID REFERENCES users(id) ON DELETE SET NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC')
);
CREATE INDEX idx_salary_history_user ON salary_history(user_id, changed_at DESC);

-- ========== KỲ LƯƠNG ==========

CREATE SEQUENCE payroll_period_code_seq;
CREATE TABLE payroll_periods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(40) NOT NULL UNIQUE,
    period_month DATE NOT NULL CHECK (EXTRACT(DAY FROM period_month) = 1),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    store_id UUID REFERENCES stores(id),            -- null = Văn phòng (nhân viên Global)
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'CALCULATED', 'PENDING_APPROVAL', 'APPROVED', 'PAID', 'CANCELLED')),
    standard_days NUMERIC(4,1),
    settings_snapshot JSONB,
    attendance_synced_at TIMESTAMP,
    attendance_synced_by UUID REFERENCES users(id) ON DELETE SET NULL,
    calculated_at TIMESTAMP,
    calculated_by UUID REFERENCES users(id) ON DELETE SET NULL,
    submitted_at TIMESTAMP,
    submitted_by UUID REFERENCES users(id) ON DELETE SET NULL,
    approved_at TIMESTAMP,
    approved_by UUID REFERENCES users(id) ON DELETE SET NULL,
    paid_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancelled_by UUID REFERENCES users(id) ON DELETE SET NULL,
    cancel_reason TEXT,
    last_rejection_reason TEXT,
    note TEXT,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT chk_payroll_periods_dates CHECK (end_date >= start_date)
);
CREATE UNIQUE INDEX uq_payroll_periods_scope ON payroll_periods(period_month, coalesce(store_id, '00000000-0000-0000-0000-000000000000'))
    WHERE status <> 'CANCELLED';
CREATE INDEX idx_payroll_periods_status ON payroll_periods(status, period_month DESC);

ALTER TABLE attendance_locks ADD CONSTRAINT fk_attendance_locks_period
    FOREIGN KEY (payroll_period_id) REFERENCES payroll_periods(id) ON DELETE CASCADE;

-- ========== BẢNG LƯƠNG TỪNG NHÂN VIÊN ==========

-- Thông tin nhân viên + số liệu công được chụp lại (snapshot) để phiếu lương đã phát hành giữ nguyên dữ liệu kỳ đó.
CREATE TABLE payroll_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    period_id UUID NOT NULL REFERENCES payroll_periods(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    period_month DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,          -- false khi kỳ lương bị hủy
    employee_code VARCHAR(30),
    full_name VARCHAR(255),
    employee_type VARCHAR(10) NOT NULL CHECK (employee_type IN ('STORE', 'GLOBAL')),
    store_id UUID,
    store_name VARCHAR(255),
    department_name VARCHAR(255),
    position_name VARCHAR(255),
    salary_structure_id UUID REFERENCES salary_structures(id),
    pay_method VARCHAR(20),
    bank_name VARCHAR(100),
    bank_account VARCHAR(50),
    base_salary NUMERIC(15,0) NOT NULL DEFAULT 0,
    standard_days NUMERIC(4,1) NOT NULL DEFAULT 0,
    window_start DATE NOT NULL,
    window_end DATE NOT NULL,
    workdays_in_window NUMERIC(5,1) NOT NULL DEFAULT 0,
    worked_days NUMERIC(5,1) NOT NULL DEFAULT 0,
    off_day_worked_days NUMERIC(5,1) NOT NULL DEFAULT 0,
    worked_minutes INT NOT NULL DEFAULT 0,
    paid_leave_days NUMERIC(5,1) NOT NULL DEFAULT 0,
    unpaid_leave_days NUMERIC(5,1) NOT NULL DEFAULT 0,
    holiday_days NUMERIC(5,1) NOT NULL DEFAULT 0,
    unverified_days NUMERIC(5,1) NOT NULL DEFAULT 0,
    confirmed_absent_days NUMERIC(5,1) NOT NULL DEFAULT 0,
    payable_days NUMERIC(5,1) NOT NULL DEFAULT 0,
    late_count INT NOT NULL DEFAULT 0,
    early_count INT NOT NULL DEFAULT 0,
    late_minutes INT NOT NULL DEFAULT 0,
    early_minutes INT NOT NULL DEFAULT 0,
    missing_check_out_count INT NOT NULL DEFAULT 0,
    missing_check_in_count INT NOT NULL DEFAULT 0,
    approved_excuse_count INT NOT NULL DEFAULT 0,
    pending_request_count INT NOT NULL DEFAULT 0,
    ot_weekday_hours NUMERIC(6,2) NOT NULL DEFAULT 0,
    ot_weekend_hours NUMERIC(6,2) NOT NULL DEFAULT 0,
    ot_holiday_hours NUMERIC(6,2) NOT NULL DEFAULT 0,
    base_pay NUMERIC(15,0) NOT NULL DEFAULT 0,
    allowance_total NUMERIC(15,0) NOT NULL DEFAULT 0,
    bonus_total NUMERIC(15,0) NOT NULL DEFAULT 0,
    overtime_pay NUMERIC(15,0) NOT NULL DEFAULT 0,
    deduction_total NUMERIC(15,0) NOT NULL DEFAULT 0,
    gross_pay NUMERIC(15,0) NOT NULL DEFAULT 0,
    net_pay NUMERIC(15,0) NOT NULL DEFAULT 0,
    -- NEEDS_REVIEW: còn ngày công chưa xác minh / đơn chờ duyệt / thực nhận âm; CONFIRMED: HR đã xác nhận ngày công.
    review_status VARCHAR(20) NOT NULL DEFAULT 'OK' CHECK (review_status IN ('OK', 'NEEDS_REVIEW', 'CONFIRMED')),
    review_note TEXT,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID' CHECK (payment_status IN ('UNPAID', 'PAID')),
    attendance_snapshot JSONB,
    calculation_detail JSONB,
    synced_at TIMESTAMP,
    calculated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMP,
    CONSTRAINT uq_payroll_records_period_user UNIQUE (period_id, user_id)
);
-- Không tính trùng: một nhân viên chỉ thuộc một bảng lương còn hiệu lực trong mỗi tháng.
CREATE UNIQUE INDEX uq_payroll_records_user_month ON payroll_records(user_id, period_month) WHERE active;
CREATE INDEX idx_payroll_records_user ON payroll_records(user_id, period_month DESC);

CREATE TABLE payroll_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    record_id UUID NOT NULL REFERENCES payroll_records(id) ON DELETE CASCADE,
    component_id UUID REFERENCES payroll_components(id),
    code VARCHAR(40),
    name VARCHAR(150) NOT NULL,
    kind VARCHAR(20) NOT NULL CHECK (kind IN ('ALLOWANCE', 'BONUS', 'OVERTIME', 'DEDUCTION')),
    calc_method VARCHAR(30),
    quantity NUMERIC(10,2),
    rate NUMERIC(15,4),
    amount NUMERIC(15,0) NOT NULL CHECK (amount >= 0),
    -- STRUCTURE: từ hồ sơ lương; SYSTEM: tăng ca; MANUAL: HR nhập trong kỳ; ADJUSTMENT: điều chỉnh từ kỳ đã chốt.
    source VARCHAR(20) NOT NULL CHECK (source IN ('STRUCTURE', 'SYSTEM', 'MANUAL', 'ADJUSTMENT')),
    adjustment_id UUID,
    note TEXT,
    sort_order INT NOT NULL DEFAULT 100,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC')
);
CREATE INDEX idx_payroll_items_record ON payroll_items(record_id, sort_order);

-- ========== ĐIỀU CHỈNH / PHÊ DUYỆT / THANH TOÁN ==========

CREATE TABLE payroll_adjustments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    record_id UUID NOT NULL REFERENCES payroll_records(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    -- IN_PERIOD: sửa khi chưa chốt (ghi lịch sử); AFTER_CLOSE: kỳ đã chốt → khoản truy lĩnh / truy thu ở kỳ sau.
    phase VARCHAR(20) NOT NULL CHECK (phase IN ('IN_PERIOD', 'AFTER_CLOSE')),
    action VARCHAR(30) NOT NULL CHECK (action IN ('ADD_ITEM', 'REMOVE_ITEM', 'CONFIRM_DAYS', 'CORRECTION')),
    kind VARCHAR(20) CHECK (kind IS NULL OR kind IN ('ALLOWANCE', 'BONUS', 'DEDUCTION')),
    name VARCHAR(150),
    amount NUMERIC(15,0) CHECK (amount IS NULL OR amount >= 0),
    old_value JSONB,
    new_value JSONB,
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'APPLIED' CHECK (status IN ('APPLIED', 'PENDING', 'CANCELLED')),
    applied_record_id UUID REFERENCES payroll_records(id) ON DELETE SET NULL,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC')
);
CREATE INDEX idx_payroll_adjustments_record ON payroll_adjustments(record_id, created_at DESC);
CREATE INDEX idx_payroll_adjustments_pending ON payroll_adjustments(user_id) WHERE phase = 'AFTER_CLOSE' AND status = 'PENDING';
ALTER TABLE payroll_items ADD CONSTRAINT fk_payroll_items_adjustment
    FOREIGN KEY (adjustment_id) REFERENCES payroll_adjustments(id) ON DELETE SET NULL;

CREATE TABLE payroll_approvals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    period_id UUID NOT NULL REFERENCES payroll_periods(id) ON DELETE CASCADE,
    action VARCHAR(30) NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20),
    note TEXT,
    actor_id UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC')
);
CREATE INDEX idx_payroll_approvals_period ON payroll_approvals(period_id, created_at);

CREATE TABLE payroll_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    record_id UUID NOT NULL UNIQUE REFERENCES payroll_records(id) ON DELETE CASCADE,   -- không trả hai lần
    amount NUMERIC(15,0) NOT NULL CHECK (amount >= 0),
    method VARCHAR(20) NOT NULL CHECK (method IN ('BANK_TRANSFER', 'CASH')),
    reference VARCHAR(100),
    paid_at TIMESTAMP NOT NULL,
    confirmed_by UUID REFERENCES users(id) ON DELETE SET NULL,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC')
);

-- ========== PHÂN QUYỀN ==========

INSERT INTO permission_groups(id, name, code, description, created_at, module_id)
SELECT (substr(md5('permission_group:PAYROLL'),1,8) || '-' || substr(md5('permission_group:PAYROLL'),9,4) || '-' ||
        substr(md5('permission_group:PAYROLL'),13,4) || '-' || substr(md5('permission_group:PAYROLL'),17,4) || '-' ||
        substr(md5('permission_group:PAYROLL'),21,12))::uuid,
       'Tính lương', 'PAYROLL', 'Hồ sơ lương, kỳ lương, phê duyệt, thanh toán, báo cáo chi phí lương', current_timestamp, m.id
FROM modules m WHERE m.code = 'HUMAN_RESOURCE'
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions(id, code, name, group_id, description, created_at)
SELECT (substr(md5('permission:' || p.code),1,8) || '-' || substr(md5('permission:' || p.code),9,4) || '-' ||
        substr(md5('permission:' || p.code),13,4) || '-' || substr(md5('permission:' || p.code),17,4) || '-' ||
        substr(md5('permission:' || p.code),21,12))::uuid,
       p.code, p.name, g.id, p.description, current_timestamp
FROM permission_groups g
CROSS JOIN (VALUES
    ('PAYROLL_VIEW', 'Xem bảng lương', 'Xem kỳ lương, bảng lương, dashboard trong phạm vi được cấp'),
    ('PAYROLL_MANAGE', 'Lập bảng lương', 'Tạo kỳ lương, đồng bộ công, tính lương, điều chỉnh, gửi duyệt'),
    ('PAYROLL_APPROVE', 'Duyệt bảng lương', 'Phê duyệt / từ chối và chốt bảng lương'),
    ('PAYROLL_PAY', 'Xác nhận thanh toán lương', 'Ghi nhận đã chi trả lương'),
    ('PAYROLL_EXPORT', 'Xuất báo cáo lương', 'Xuất Excel bảng lương, báo cáo, phiếu lương PDF'),
    ('SALARY_VIEW', 'Xem hồ sơ lương', 'Xem mức lương, phụ cấp của nhân viên trong phạm vi'),
    ('SALARY_MANAGE', 'Cập nhật hồ sơ lương', 'Tạo / thay đổi mức lương, phụ cấp, thưởng, khấu trừ'),
    ('PAYROLL_ADMIN', 'Quản trị lương', 'Cấu hình chính sách lương, khoản lương, xem hồ sơ lương bảo mật')
) AS p(code, name, description)
WHERE g.code = 'PAYROLL'
ON CONFLICT (code) DO NOTHING;

INSERT INTO roles(code, name, description) VALUES
    ('PAYROLL_APPROVER', 'Payroll Approver', 'Phê duyệt và chốt bảng lương')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id, scope)
SELECT r.id, p.id, 'ALL' FROM roles r JOIN permissions p ON p.code IN
    ('PAYROLL_VIEW', 'PAYROLL_MANAGE', 'PAYROLL_APPROVE', 'PAYROLL_PAY', 'PAYROLL_EXPORT', 'SALARY_VIEW', 'SALARY_MANAGE', 'PAYROLL_ADMIN')
WHERE r.code IN ('SUPER_ADMIN', 'ADMIN', 'HR_ADMIN')
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id, scope)
SELECT r.id, p.id, 'ALL' FROM roles r JOIN permissions p ON p.code IN
    ('PAYROLL_VIEW', 'PAYROLL_MANAGE', 'PAYROLL_EXPORT', 'SALARY_VIEW', 'SALARY_MANAGE')
WHERE r.code = 'HR_PAYROLL'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id, scope)
SELECT r.id, p.id, 'ALL' FROM roles r JOIN permissions p ON p.code IN
    ('PAYROLL_VIEW', 'PAYROLL_APPROVE', 'PAYROLL_EXPORT', 'SALARY_VIEW')
WHERE r.code = 'PAYROLL_APPROVER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Kế toán xác nhận chi trả.
INSERT INTO role_permissions(role_id, permission_id, scope)
SELECT r.id, p.id, 'ALL' FROM roles r JOIN permissions p ON p.code IN ('PAYROLL_VIEW', 'PAYROLL_PAY', 'PAYROLL_EXPORT')
WHERE r.code = 'ACCOUNTANT'
ON CONFLICT (role_id, permission_id) DO NOTHING;
