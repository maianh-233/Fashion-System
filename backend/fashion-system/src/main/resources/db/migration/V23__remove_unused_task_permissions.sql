-- Module công việc chỉ dùng TASK_VIEW và TASK_ASSIGN. Bốn quyền còn lại từ dữ liệu gốc không có tác dụng nên bỏ khỏi màn Phân quyền;
-- gán quyền theo vai trò / người dùng tự xóa theo (cascade), nhật ký role_permission_audit giữ lại với permission_id = null.
DELETE FROM permissions WHERE code IN ('TASK_CREATE', 'TASK_UPDATE', 'TASK_DELETE', 'TASK_REPORT');
