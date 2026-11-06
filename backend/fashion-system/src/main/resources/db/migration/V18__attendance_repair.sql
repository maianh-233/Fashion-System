-- Hoàn thiện module chấm công để làm đầu vào cho tính lương:
-- loại nghỉ có / không lương + hạn mức năm, số phút đi trễ / về sớm, xác minh vị trí, tăng ca có duyệt,
-- ngày lễ + ngày nghỉ hằng tuần, khóa dữ liệu công khi kỳ lương đã chốt. Các bảng chấm công chưa có dữ liệu.

-- ========== CẤU HÌNH CHUNG ==========

CREATE TABLE attendance_settings (
    id SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    weekly_off_days SMALLINT[] NOT NULL DEFAULT '{7}',          -- ISO: 1 = thứ Hai … 7 = Chủ nhật
    standard_hours_per_day NUMERIC(4,2) NOT NULL DEFAULT 8 CHECK (standard_hours_per_day > 0),
    store_radius_meters INT NOT NULL DEFAULT 300 CHECK (store_radius_meters > 0),
    require_store_location BOOLEAN NOT NULL DEFAULT TRUE,
    updated_by UUID REFERENCES users(id) ON DELETE SET NULL,
    updated_at TIMESTAMP
);
INSERT INTO attendance_settings(id) VALUES (1);

-- Giờ làm mặc định toàn chuỗi (store_id, shift_id null); cửa hàng / ca có thể ghi đè bằng dòng riêng.
INSERT INTO attendance_configs(check_in_time, check_out_time, break_duration_minutes, late_threshold_minutes,
                               early_departure_threshold_minutes)
VALUES ('08:00', '18:00', 60, 5, 5);
CREATE UNIQUE INDEX uq_attendance_configs_default ON attendance_configs((1)) WHERE store_id IS NULL AND shift_id IS NULL;
CREATE UNIQUE INDEX uq_attendance_configs_store ON attendance_configs(store_id) WHERE store_id IS NOT NULL AND shift_id IS NULL;

CREATE TABLE public_holidays (
    holiday_date DATE PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC')
);
-- Ngày lễ dương lịch cố định; Tết Âm lịch và Giỗ tổ HR thêm theo thông báo hằng năm.
INSERT INTO public_holidays(holiday_date, name) VALUES
    ('2026-01-01', 'Tết Dương lịch'), ('2026-04-30', 'Ngày Giải phóng miền Nam'), ('2026-05-01', 'Quốc tế Lao động'),
    ('2026-09-01', 'Quốc khánh (nghỉ kèm)'), ('2026-09-02', 'Quốc khánh'),
    ('2027-01-01', 'Tết Dương lịch'), ('2027-04-30', 'Ngày Giải phóng miền Nam'), ('2027-05-01', 'Quốc tế Lao động'),
    ('2027-09-02', 'Quốc khánh'), ('2027-09-03', 'Quốc khánh (nghỉ kèm)');

-- ========== BẢN GHI CHẤM CÔNG ==========

ALTER TABLE attendance_records
    ADD COLUMN expected_start TIME,
    ADD COLUMN expected_end TIME,
    ADD COLUMN late_minutes INT NOT NULL DEFAULT 0 CHECK (late_minutes >= 0),
    ADD COLUMN early_minutes INT NOT NULL DEFAULT 0 CHECK (early_minutes >= 0),
    ADD COLUMN check_in_distance_m INT,
    ADD COLUMN check_out_distance_m INT,
    ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'SELF';
ALTER TABLE attendance_records ALTER COLUMN status SET DEFAULT 'MISSING_CHECK_OUT';
ALTER TABLE attendance_records ADD CONSTRAINT chk_attendance_records_status
    CHECK (status IN ('COMPLETED', 'MISSING_CHECK_OUT', 'MISSING_CHECK_IN'));
ALTER TABLE attendance_records ADD CONSTRAINT chk_attendance_records_source
    CHECK (source IN ('SELF', 'EXCUSE', 'ADJUSTED'));

-- ========== ĐƠN GIẢI TRÌNH ==========

-- Đơn bị từ chối được nộp lại: chỉ chặn trùng khi đơn cũ còn chờ / đã duyệt.
ALTER TABLE excuse_requests DROP CONSTRAINT uq_excuse_requests;
CREATE UNIQUE INDEX uq_excuse_requests_open ON excuse_requests(user_id, attendance_date, excuse_type)
    WHERE status IN ('PENDING', 'APPROVED');
ALTER TABLE excuse_requests
    ADD COLUMN store_id UUID REFERENCES stores(id),
    ADD COLUMN approver_note TEXT;
ALTER TABLE excuse_requests ADD CONSTRAINT chk_excuse_requests_status
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'));

-- ========== NGHỈ PHÉP ==========

ALTER TABLE leave_types
    ADD COLUMN paid BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN annual_quota NUMERIC(5,1) CHECK (annual_quota IS NULL OR annual_quota >= 0);
UPDATE leave_types SET paid = (code <> 'UNPAID');
UPDATE leave_types SET annual_quota = CASE code WHEN 'ANNUAL' THEN 12 WHEN 'SICK' THEN 5 WHEN 'PERSONAL' THEN 3 END;

ALTER TABLE leave_requests
    ADD COLUMN total_days NUMERIC(5,1) NOT NULL DEFAULT 0 CHECK (total_days >= 0),
    ADD COLUMN approver_note TEXT;
ALTER TABLE leave_requests ADD CONSTRAINT chk_leave_requests_status
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'));
CREATE INDEX idx_leave_requests_store ON leave_requests(store_id, status);

-- ========== TĂNG CA ==========

CREATE SEQUENCE overtime_request_code_seq;
CREATE TABLE overtime_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(30) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    store_id UUID REFERENCES stores(id),
    work_date DATE NOT NULL,
    hours NUMERIC(4,2) NOT NULL CHECK (hours > 0 AND hours <= 12),
    approved_hours NUMERIC(4,2) CHECK (approved_hours IS NULL OR (approved_hours > 0 AND approved_hours <= hours)),
    overtime_type VARCHAR(20) NOT NULL CHECK (overtime_type IN ('WEEKDAY', 'WEEKEND', 'HOLIDAY')),
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    approved_by UUID REFERENCES users(id) ON DELETE SET NULL,
    approved_at TIMESTAMP,
    rejection_reason TEXT,
    approver_note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMP
);
CREATE UNIQUE INDEX uq_overtime_requests_open ON overtime_requests(user_id, work_date) WHERE status IN ('PENDING', 'APPROVED');
CREATE INDEX idx_overtime_requests_status ON overtime_requests(status, work_date DESC);
CREATE INDEX idx_overtime_requests_user ON overtime_requests(user_id, work_date DESC);

-- ========== KHÓA DỮ LIỆU CÔNG ==========

-- Khi kỳ lương gửi duyệt, dữ liệu công của nhân viên trong tháng đó bị khóa (không sửa / duyệt đơn được nữa).
CREATE TABLE attendance_locks (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    period_month DATE NOT NULL CHECK (EXTRACT(DAY FROM period_month) = 1),
    payroll_period_id UUID NOT NULL,
    locked_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    PRIMARY KEY (user_id, period_month)
);
CREATE INDEX idx_attendance_locks_period ON attendance_locks(payroll_period_id);
