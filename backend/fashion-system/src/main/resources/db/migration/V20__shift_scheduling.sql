-- Xếp ca & đổi ca: ca mẫu theo cửa hàng (dùng lại bảng shifts), lịch làm theo ngày (nháp → công bố), đăng ký ngày
-- rảnh / bận của nhân viên, yêu cầu đổi / nhường ca (đồng nghiệp đồng ý → quản lý duyệt). Lịch đã công bố là giờ làm
-- dự kiến khi chấm công (đi trễ / về sớm / vắng tính theo ca). Bảng shifts / employee_shifts chưa có dữ liệu.

-- ========== CA MẪU ==========

ALTER TABLE shifts
    ADD COLUMN break_minutes INT NOT NULL DEFAULT 0 CHECK (break_minutes >= 0 AND break_minutes <= 240),
    ADD COLUMN color VARCHAR(20) NOT NULL DEFAULT 'amber',
    ADD COLUMN sort_order INT NOT NULL DEFAULT 0;
ALTER TABLE shifts ALTER COLUMN active SET NOT NULL;
ALTER TABLE shifts ADD CONSTRAINT chk_shifts_type CHECK (shift_type IN ('MORNING', 'AFTERNOON', 'EVENING', 'FULL_DAY', 'NIGHT', 'CUSTOM'));
ALTER TABLE shifts ADD CONSTRAINT chk_shifts_times CHECK (start_time <> end_time);
-- Ca dùng chung toàn chuỗi (store_id null) cũng không được trùng mã.
CREATE UNIQUE INDEX uq_shifts_global_code ON shifts(code) WHERE store_id IS NULL;

INSERT INTO shifts(store_id, code, name, shift_type, start_time, end_time, break_minutes, color, sort_order, active) VALUES
    (NULL, 'SANG', 'Ca sáng', 'MORNING', '08:00', '14:00', 0, 'sky', 1, TRUE),
    (NULL, 'CHIEU', 'Ca chiều', 'AFTERNOON', '14:00', '22:00', 30, 'violet', 2, TRUE),
    (NULL, 'HANHCHINH', 'Ca hành chính', 'FULL_DAY', '08:00', '17:00', 60, 'amber', 3, TRUE);

-- ========== LỊCH LÀM THEO NGÀY ==========

-- Giờ ca được chụp lại từ ca mẫu khi xếp, nên sửa ca mẫu không làm đổi lịch đã xếp. Mỗi nhân viên tối đa một ca / ngày
-- (khớp attendance_records một bản ghi / ngày).
CREATE TABLE shift_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    work_date DATE NOT NULL,
    shift_id UUID REFERENCES shifts(id) ON DELETE SET NULL,
    shift_name VARCHAR(100) NOT NULL,
    color VARCHAR(20) NOT NULL DEFAULT 'amber',
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    break_minutes INT NOT NULL DEFAULT 0 CHECK (break_minutes >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED')),
    note VARCHAR(500),
    published_at TIMESTAMP,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    updated_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMP,
    CONSTRAINT uq_shift_schedules_user_date UNIQUE (user_id, work_date),
    CONSTRAINT chk_shift_schedules_times CHECK (start_time <> end_time)
);
CREATE INDEX idx_shift_schedules_store_date ON shift_schedules(store_id, work_date);
CREATE INDEX idx_shift_schedules_status ON shift_schedules(status, work_date);

-- ========== ĐĂNG KÝ NGÀY RẢNH / BẬN ==========

CREATE TABLE shift_availabilities (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    work_date DATE NOT NULL,
    kind VARCHAR(20) NOT NULL CHECK (kind IN ('AVAILABLE', 'UNAVAILABLE')),
    preferred_shift_id UUID REFERENCES shifts(id) ON DELETE SET NULL,
    note VARCHAR(300),
    updated_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    PRIMARY KEY (user_id, work_date)
);
CREATE INDEX idx_shift_availabilities_date ON shift_availabilities(work_date);

-- ========== ĐỔI / NHƯỜNG CA ==========

-- target_schedule_id null = nhường ca (đồng nghiệp làm thay); có giá trị = hai người đổi ca cho nhau.
-- Luồng: PENDING_PEER (chờ đồng nghiệp) → PENDING_APPROVAL (chờ quản lý) → APPROVED; hoặc DECLINED / REJECTED / CANCELLED.
CREATE SEQUENCE shift_swap_code_seq;
CREATE TABLE shift_swap_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(30) NOT NULL UNIQUE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    requester_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    requester_schedule_id UUID REFERENCES shift_schedules(id) ON DELETE SET NULL,
    target_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_schedule_id UUID REFERENCES shift_schedules(id) ON DELETE SET NULL,
    swap_type VARCHAR(20) NOT NULL CHECK (swap_type IN ('SWAP', 'GIVE_AWAY')),
    -- Chụp lại thông tin ca lúc tạo đơn để vẫn hiển thị được khi lịch đã thay đổi.
    requester_date DATE NOT NULL,
    requester_shift VARCHAR(150) NOT NULL,
    target_date DATE,
    target_shift VARCHAR(150),
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_PEER'
        CHECK (status IN ('PENDING_PEER', 'PENDING_APPROVAL', 'APPROVED', 'DECLINED', 'REJECTED', 'CANCELLED')),
    peer_responded_at TIMESTAMP,
    peer_note VARCHAR(1000),
    decided_by UUID REFERENCES users(id) ON DELETE SET NULL,
    decided_at TIMESTAMP,
    rejection_reason VARCHAR(1000),
    approver_note VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMP,
    CONSTRAINT chk_shift_swap_users CHECK (requester_id <> target_user_id)
);
-- Một ca chỉ có một đơn đổi đang mở.
CREATE UNIQUE INDEX uq_shift_swap_open ON shift_swap_requests(requester_schedule_id)
    WHERE status IN ('PENDING_PEER', 'PENDING_APPROVAL');
CREATE INDEX idx_shift_swap_store_status ON shift_swap_requests(store_id, status, created_at DESC);
CREATE INDEX idx_shift_swap_requester ON shift_swap_requests(requester_id, created_at DESC);
CREATE INDEX idx_shift_swap_target ON shift_swap_requests(target_user_id, created_at DESC);

-- ========== QUYỀN ==========

INSERT INTO permissions(id, code, name, group_id, description, created_at)
SELECT (substr(md5('permission:' || p.code),1,8) || '-' || substr(md5('permission:' || p.code),9,4) || '-' ||
        substr(md5('permission:' || p.code),13,4) || '-' || substr(md5('permission:' || p.code),17,4) || '-' ||
        substr(md5('permission:' || p.code),21,12))::uuid,
       p.code, p.name, g.id, p.description, current_timestamp
FROM permission_groups g
CROSS JOIN (VALUES
    ('SHIFT_VIEW', 'Xem lịch làm', 'Xem lịch xếp ca của cửa hàng và đối chiếu chấm công'),
    ('SHIFT_MANAGE', 'Xếp ca', 'Xếp ca, công bố lịch làm, quản lý ca mẫu và duyệt đổi ca')
) AS p(code, name, description)
WHERE g.code = 'ATTENDANCE'
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id, scope)
SELECT r.id, p.id, 'ALL' FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('SUPER_ADMIN', 'ADMIN', 'HR_ADMIN', 'HR_ATTENDANCE_ADMIN', 'MANAGER') AND p.code IN ('SHIFT_VIEW', 'SHIFT_MANAGE')
ON CONFLICT (role_id, permission_id) DO NOTHING;
