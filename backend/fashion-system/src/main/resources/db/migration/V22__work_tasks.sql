-- Quản lý công việc: cấp trên giao việc cho nhân viên → nhân viên làm theo checklist và xác nhận hoàn thành → cấp trên được
-- báo và có thể trả lại. Giao cho nhiều người thì mỗi người một dòng riêng, cùng batch_id. "Quá hạn" tính từ due_at, không lưu.

CREATE SEQUENCE work_task_code_seq;

CREATE TABLE work_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(30) NOT NULL UNIQUE,
    batch_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    priority VARCHAR(10) NOT NULL DEFAULT 'NORMAL' CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    due_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'TODO' CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE', 'CANCELLED')),
    assigner_id UUID REFERENCES users(id) ON DELETE SET NULL,
    assignee_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    -- Cửa hàng của người nhận lúc giao việc; null là nhân viên Global.
    store_id UUID REFERENCES stores(id),
    completion_note VARCHAR(1000),
    completed_at TIMESTAMP,
    return_reason VARCHAR(1000),
    return_count INT NOT NULL DEFAULT 0,
    due_soon_notified_at TIMESTAMP,
    overdue_notified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    updated_at TIMESTAMP,
    CONSTRAINT chk_work_tasks_not_self CHECK (assigner_id IS NULL OR assigner_id <> assignee_id)
);
CREATE INDEX idx_work_tasks_assignee ON work_tasks(assignee_id, status, due_at);
CREATE INDEX idx_work_tasks_assigner ON work_tasks(assigner_id, status, created_at DESC);
CREATE INDEX idx_work_tasks_store ON work_tasks(store_id, status, created_at DESC);
CREATE INDEX idx_work_tasks_batch ON work_tasks(batch_id);
-- Quét nhắc hạn chỉ cần các việc còn mở có hạn chót.
CREATE INDEX idx_work_tasks_open_due ON work_tasks(due_at) WHERE status IN ('TODO', 'IN_PROGRESS') AND due_at IS NOT NULL;

CREATE TABLE work_task_checklist_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES work_tasks(id) ON DELETE CASCADE,
    content VARCHAR(500) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    done BOOLEAN NOT NULL DEFAULT FALSE,
    done_at TIMESTAMP
);
CREATE INDEX idx_work_task_checklist_task ON work_task_checklist_items(task_id, sort_order);

CREATE TABLE work_task_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES work_tasks(id) ON DELETE CASCADE,
    author_id UUID REFERENCES users(id) ON DELETE SET NULL,
    content VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC')
);
CREATE INDEX idx_work_task_comments_task ON work_task_comments(task_id, created_at);

CREATE TABLE work_task_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES work_tasks(id) ON DELETE CASCADE,
    action VARCHAR(20) NOT NULL CHECK (action IN ('CREATED', 'UPDATED', 'STARTED', 'COMPLETED', 'RETURNED', 'CANCELLED')),
    from_status VARCHAR(20),
    to_status VARCHAR(20),
    note VARCHAR(1000),
    actor_id UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'UTC')
);
CREATE INDEX idx_work_task_history_task ON work_task_history(task_id, created_at);

-- ========== MODULE VÀ QUYỀN ==========

INSERT INTO modules(id, active, code, created_at, description, icon, name, sort_order)
SELECT (substr(md5('module:WORK'),1,8) || '-' || substr(md5('module:WORK'),9,4) || '-' ||
        substr(md5('module:WORK'),13,4) || '-' || substr(md5('module:WORK'),17,4) || '-' ||
        substr(md5('module:WORK'),21,12))::uuid,
       true, 'WORK', current_timestamp, 'Giao việc, theo dõi tiến độ và thống kê công việc', 'list-checks', 'Công việc', 45
ON CONFLICT DO NOTHING;

-- Nhóm quyền TASK đã có sẵn từ dữ liệu gốc (thuộc Nhân sự, chưa được code nào dùng): chuyển sang module mới.
UPDATE permission_groups
SET module_id = (SELECT id FROM modules WHERE code = 'WORK'),
    name = 'Quản lý công việc',
    description = 'Giao việc cho nhân viên, trả lại việc chưa đạt, xem tiến độ và thống kê'
WHERE code = 'TASK';

UPDATE permissions SET description = 'Xem mọi công việc trong phạm vi và thống kê theo nhân viên' WHERE code = 'TASK_VIEW';
UPDATE permissions SET description = 'Giao việc cho nhân viên trong phạm vi, trả lại hoặc hủy công việc' WHERE code = 'TASK_ASSIGN';

-- Nhân viên chỉ thấy việc của mình; dữ liệu gốc cấp TASK_VIEW / TASK_REPORT cho STAFF khi các quyền này chưa có tác dụng.
DELETE FROM role_permissions
WHERE role_id = (SELECT id FROM roles WHERE code = 'STAFF')
  AND permission_id IN (SELECT id FROM permissions WHERE code IN ('TASK_VIEW', 'TASK_REPORT'));

INSERT INTO role_permissions(role_id, permission_id, scope)
SELECT r.id, p.id, 'ALL' FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('SUPER_ADMIN', 'ADMIN', 'MANAGER', 'HR_ADMIN') AND p.code IN ('TASK_VIEW', 'TASK_ASSIGN')
ON CONFLICT (role_id, permission_id) DO NOTHING;
