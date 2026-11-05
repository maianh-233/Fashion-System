-- Quản lý chấm công và nghỉ phép
-- Shift configuration, attendance tracking, excuse/leave requests, audit logging

-- ========== SEQUENCES ==========
CREATE SEQUENCE attendance_excuse_code_seq;
CREATE SEQUENCE leave_request_code_seq;

-- ========== SHIFTS & CONFIGURATION ==========

CREATE TABLE shifts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID REFERENCES stores(id) ON DELETE CASCADE,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    shift_type VARCHAR(30) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMP,
    CONSTRAINT uq_shifts_store_code UNIQUE(store_id, code)
);
CREATE INDEX idx_shifts_store ON shifts(store_id, active);

CREATE TABLE employee_shifts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    shift_id UUID NOT NULL REFERENCES shifts(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMP,
    CONSTRAINT chk_employee_shifts_dates CHECK(end_date IS NULL OR end_date >= start_date),
    CONSTRAINT uq_employee_shifts_dates UNIQUE(user_id, shift_id, start_date)
);
CREATE INDEX idx_employee_shifts_user ON employee_shifts(user_id, start_date DESC);
CREATE INDEX idx_employee_shifts_shift ON employee_shifts(shift_id);

CREATE TABLE attendance_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID REFERENCES stores(id) ON DELETE CASCADE,
    shift_id UUID REFERENCES shifts(id) ON DELETE CASCADE,
    check_in_time TIME NOT NULL,
    check_out_time TIME NOT NULL,
    break_duration_minutes INT DEFAULT 60,
    late_threshold_minutes INT DEFAULT 5,
    early_departure_threshold_minutes INT DEFAULT 5,
    auto_approve_excess_hours BOOLEAN DEFAULT FALSE,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMP,
    CONSTRAINT uq_attendance_configs UNIQUE(store_id, shift_id)
);
CREATE INDEX idx_attendance_configs_store ON attendance_configs(store_id);

-- ========== ATTENDANCE RECORDS ==========

CREATE TABLE attendance_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    attendance_date DATE NOT NULL,
    store_id UUID REFERENCES stores(id),
    shift_id UUID REFERENCES shifts(id),
    check_in_time TIMESTAMP,
    check_out_time TIMESTAMP,
    check_in_location_lat NUMERIC(9,6),
    check_in_location_lon NUMERIC(9,6),
    check_out_location_lat NUMERIC(9,6),
    check_out_location_lon NUMERIC(9,6),
    status VARCHAR(30) NOT NULL DEFAULT 'PRESENT',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMP,
    CONSTRAINT uq_attendance_records UNIQUE(user_id, attendance_date),
    CONSTRAINT chk_attendance_records_times CHECK(check_out_time IS NULL OR check_out_time > check_in_time)
);
CREATE INDEX idx_attendance_records_user_date ON attendance_records(user_id, attendance_date DESC);
CREATE INDEX idx_attendance_records_store_date ON attendance_records(store_id, attendance_date DESC);
CREATE INDEX idx_attendance_records_status ON attendance_records(status, attendance_date DESC);

CREATE TABLE attendance_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attendance_record_id UUID REFERENCES attendance_records(id) ON DELETE SET NULL,
    action VARCHAR(50) NOT NULL,
    old_data JSONB,
    new_data JSONB,
    reason TEXT,
    actor_id UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC')
);
CREATE INDEX idx_attendance_audit_logs_record ON attendance_audit_logs(attendance_record_id, created_at DESC);

CREATE TABLE attendance_reminders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reminder_type VARCHAR(30) NOT NULL,
    reminder_date DATE NOT NULL,
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC')
);
CREATE INDEX idx_attendance_reminders_user_date ON attendance_reminders(user_id, reminder_date DESC);

-- ========== EXCUSE REQUESTS ==========

CREATE TABLE excuse_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(30) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    attendance_date DATE NOT NULL,
    excuse_type VARCHAR(30) NOT NULL,
    proposed_check_in TIMESTAMP,
    proposed_check_out TIMESTAMP,
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by UUID REFERENCES users(id) ON DELETE SET NULL,
    approved_at TIMESTAMP,
    rejection_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMP,
    CONSTRAINT uq_excuse_requests UNIQUE(user_id, attendance_date, excuse_type),
    CONSTRAINT chk_excuse_types CHECK(excuse_type IN ('FORGOT_CHECK_IN', 'FORGOT_CHECK_OUT'))
);
CREATE INDEX idx_excuse_requests_status ON excuse_requests(status, created_at DESC);
CREATE INDEX idx_excuse_requests_user ON excuse_requests(user_id, created_at DESC);

-- ========== LEAVE TYPES & REQUESTS ==========

CREATE TABLE leave_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    requires_approval BOOLEAN DEFAULT TRUE,
    requires_documents BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMP
);
CREATE INDEX idx_leave_types_active ON leave_types(active);

CREATE TABLE leave_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(30) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    leave_type_id UUID NOT NULL REFERENCES leave_types(id),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_half_day_start BOOLEAN DEFAULT FALSE,
    is_half_day_end BOOLEAN DEFAULT FALSE,
    store_id UUID REFERENCES stores(id),
    shift_id UUID REFERENCES shifts(id),
    reason TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by UUID REFERENCES users(id) ON DELETE SET NULL,
    approved_at TIMESTAMP,
    rejection_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMP,
    CONSTRAINT chk_leave_dates CHECK(end_date >= start_date),
    CONSTRAINT chk_half_days CHECK(
        NOT (is_half_day_start AND is_half_day_end AND start_date = end_date)
    )
);
CREATE INDEX idx_leave_requests_status ON leave_requests(status, created_at DESC);
CREATE INDEX idx_leave_requests_user ON leave_requests(user_id, start_date DESC);
CREATE INDEX idx_leave_requests_date_range ON leave_requests(start_date, end_date);

-- ========== PERMISSIONS ==========

INSERT INTO permission_groups(id, name, code, description, created_at, module_id)
SELECT (substr(md5('permission_group:ATTENDANCE'),1,8) || '-' || substr(md5('permission_group:ATTENDANCE'),9,4) || '-' ||
        substr(md5('permission_group:ATTENDANCE'),13,4) || '-' || substr(md5('permission_group:ATTENDANCE'),17,4) || '-' ||
        substr(md5('permission_group:ATTENDANCE'),21,12))::uuid,
       'Chấm Công', 'ATTENDANCE', 'Chấm công, nghỉ phép, bảng công, đơn giải trình', current_timestamp, m.id
FROM modules m WHERE m.code = 'HUMAN_RESOURCE'
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions(id, code, name, group_id, description, created_at)
SELECT (substr(md5('permission:' || p.code),1,8) || '-' || substr(md5('permission:' || p.code),9,4) || '-' ||
        substr(md5('permission:' || p.code),13,4) || '-' || substr(md5('permission:' || p.code),17,4) || '-' ||
        substr(md5('permission:' || p.code),21,12))::uuid,
       p.code, p.name, g.id, p.description, current_timestamp
FROM permission_groups g
CROSS JOIN (VALUES
    ('ATTENDANCE_VIEW', 'Xem chấm công', 'Xem bảng công, lịch sử chấm công'),
    ('ATTENDANCE_MANAGE', 'Quản lý chấm công', 'Điều chỉnh bảng công, duyệt đơn giải trình'),
    ('ATTENDANCE_EXPORT', 'Xuất chấm công', 'Xuất bảng công ra Excel'),
    ('LEAVE_VIEW', 'Xem nghỉ phép', 'Xem đơn xin nghỉ phép và số dư'),
    ('LEAVE_REQUEST', 'Tạo đơn xin nghỉ', 'Tạo và quản lý đơn xin nghỉ phép cá nhân'),
    ('LEAVE_APPROVE', 'Duyệt nghỉ phép', 'Phê duyệt hoặc từ chối đơn xin nghỉ phép'),
    ('ATTENDANCE_ADMIN', 'Quản trị chấm công', 'Toàn quyền quản lý chấm công, cấu hình shift, quy tắc')
) AS p(code, name, description)
WHERE g.code = 'ATTENDANCE'
ON CONFLICT (code) DO NOTHING;

INSERT INTO roles(code, name, description) VALUES
    ('HR_ATTENDANCE_ADMIN', 'HR Attendance Admin', 'Quản lý chấm công, cấu hình shift và quy tắc'),
    ('HR_PAYROLL', 'HR Payroll', 'Xem bảng công để tính lương')
ON CONFLICT (code) DO NOTHING;

-- Assign permissions to existing roles
INSERT INTO role_permissions(role_id, permission_id, scope)
SELECT r.id, p.id, 'ALL' FROM roles r CROSS JOIN permissions p
WHERE (r.code IN ('SUPER_ADMIN', 'ADMIN', 'HR_ADMIN', 'HR_ATTENDANCE_ADMIN') AND p.code LIKE 'ATTENDANCE\_%')
   OR (r.code IN ('SUPER_ADMIN', 'ADMIN', 'HR_ADMIN', 'HR_ATTENDANCE_ADMIN') AND p.code LIKE 'LEAVE\_%')
   OR (r.code = 'HR_PAYROLL' AND p.code IN ('ATTENDANCE_VIEW', 'ATTENDANCE_EXPORT'))
   OR (r.code = 'MANAGER' AND p.code LIKE 'ATTENDANCE\_%')
   OR (r.code = 'MANAGER' AND p.code IN ('LEAVE_VIEW', 'LEAVE_APPROVE'))
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Assign LEAVE_REQUEST to all employees
INSERT INTO role_permissions(role_id, permission_id, scope)
SELECT r.id, p.id, 'SELF' FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('EMPLOYEE', 'MANAGER') AND p.code IN ('LEAVE_REQUEST', 'LEAVE_VIEW', 'ATTENDANCE_VIEW')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ========== DEFAULT LEAVE TYPES ==========

INSERT INTO leave_types(id, code, name, description, requires_approval, requires_documents, active, created_at) VALUES
    (gen_random_uuid(), 'ANNUAL', 'Nghỉ phép năm', 'Nghỉ phép hàng năm', TRUE, FALSE, TRUE, now() AT TIME ZONE 'UTC'),
    (gen_random_uuid(), 'SICK', 'Nghỉ ốm', 'Nghỉ ốm có lương', TRUE, TRUE, TRUE, now() AT TIME ZONE 'UTC'),
    (gen_random_uuid(), 'UNPAID', 'Nghỉ không lương', 'Nghỉ không lương', TRUE, FALSE, TRUE, now() AT TIME ZONE 'UTC'),
    (gen_random_uuid(), 'PERSONAL', 'Nghỉ việc riêng', 'Nghỉ vì lý do cá nhân', TRUE, FALSE, TRUE, now() AT TIME ZONE 'UTC'),
    (gen_random_uuid(), 'MATERNITY', 'Nghỉ BHXH', 'Nghỉ sinh con', FALSE, FALSE, TRUE, now() AT TIME ZONE 'UTC'),
    (gen_random_uuid(), 'FUNERAL', 'Nghỉ tang', 'Nghỉ tang thân nhân', FALSE, FALSE, TRUE, now() AT TIME ZONE 'UTC')
ON CONFLICT DO NOTHING;
