import { useEffect, useMemo, useState } from "react";
import Button from "../../common/Button";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../common/AdminDialog";
import { AdminField, AdminFormSection } from "../common/AdminForm";

const emptyTask = {
  id: null,
  title: "",
  description: "",
  assigneeId: "",
  priority: "MEDIUM",
  status: "TODO",
  startDate: "",
  dueDate: "",
  estimatedHours: 8,
  bonus: 0,
  performanceWeight: 1,
};

export default function TaskAssignmentDialog({ open, task, employees = [], managerId, onClose, onSave }) {
  const isEdit = Boolean(task?.id);
  const [form, setForm] = useState(emptyTask);
  const [submitted, setSubmitted] = useState(false);

  useEffect(() => {
    if (!open) return;
    setForm(task ? { ...emptyTask, ...task } : { ...emptyTask, managerId });
    setSubmitted(false);
  }, [open, task, managerId]);

  const errors = useMemo(() => ({
    title: form.title.trim() ? "" : "Vui lòng nhập tên công việc.",
    assigneeId: form.assigneeId ? "" : "Vui lòng chọn nhân viên thực hiện.",
    dates: form.startDate && form.dueDate && form.dueDate < form.startDate ? "Hạn hoàn thành phải sau ngày bắt đầu." : "",
  }), [form]);
  const isValid = !Object.values(errors).some(Boolean);

  const update = (field, value) => setForm((current) => ({ ...current, [field]: value }));
  const submit = () => {
    setSubmitted(true);
    if (!isValid) return;
    onSave?.(form);
  };

  return (
    <AdminDialog open={open} onClose={onClose} size="lg">
      <AdminDialogHeader
        title={isEdit ? "Chỉnh sửa công việc" : "Giao công việc mới"}
        description="Trưởng nhóm giao việc cho nhân viên thuộc phạm vi quản lý."
        onClose={onClose}
      />
      <AdminDialogBody className="space-y-5">
        <AdminFormSection title="Nội dung công việc">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div className="sm:col-span-2">
              <AdminField label="Tên công việc *" value={form.title} onChange={(event) => update("title", event.target.value)} placeholder="Ví dụ: Kiểm kê kho cuối tháng" />
              {submitted && errors.title && <p className="rbac-field-error">{errors.title}</p>}
            </div>
            <AdminField as="textarea" rows="4" label="Mô tả và kết quả cần đạt" value={form.description} onChange={(event) => update("description", event.target.value)} containerClassName="sm:col-span-2" placeholder="Mô tả phạm vi, yêu cầu và tiêu chí hoàn thành..." />
          </div>
        </AdminFormSection>

        <AdminFormSection title="Phân công và thời hạn">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <AdminField as="select" label="Nhân viên thực hiện *" value={form.assigneeId} onChange={(event) => update("assigneeId", event.target.value)}>
                <option value="">Chọn nhân viên dưới quyền</option>
                {employees.map((employee) => <option key={employee.id} value={employee.id}>{employee.name} — {employee.department}</option>)}
              </AdminField>
              {submitted && errors.assigneeId && <p className="rbac-field-error">{errors.assigneeId}</p>}
            </div>
            <AdminField as="select" label="Mức độ ưu tiên" value={form.priority} onChange={(event) => update("priority", event.target.value)}>
              <option value="LOW">Thấp</option><option value="MEDIUM">Trung bình</option><option value="HIGH">Cao</option><option value="URGENT">Khẩn cấp</option>
            </AdminField>
            <AdminField label="Ngày bắt đầu" type="date" value={form.startDate} onChange={(event) => update("startDate", event.target.value)} />
            <AdminField label="Hạn hoàn thành" type="date" value={form.dueDate} onChange={(event) => update("dueDate", event.target.value)} />
            {submitted && errors.dates && <p className="rbac-field-error sm:col-span-2">{errors.dates}</p>}
          </div>
        </AdminFormSection>

        <AdminFormSection title="Định mức hiệu suất" description="Dùng để so sánh kế hoạch và thực tế khi tổng hợp hiệu suất.">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <AdminField label="Giờ dự kiến" type="number" min="1" value={form.estimatedHours} onChange={(event) => update("estimatedHours", Number(event.target.value))} />
            <AdminField label="Thưởng hoàn thành (₫)" type="number" min="0" step="50000" value={form.bonus} onChange={(event) => update("bonus", Number(event.target.value))} />
            <AdminField label="Trọng số hiệu suất" type="number" min="0.5" max="3" step="0.5" value={form.performanceWeight} onChange={(event) => update("performanceWeight", Number(event.target.value))} />
          </div>
        </AdminFormSection>
      </AdminDialogBody>
      <AdminDialogFooter>
        <Button type="button" variant="secondary" onClick={onClose} className="rounded-xl px-5 py-2">Hủy</Button>
        <Button type="button" variant="primary" onClick={submit} className="rounded-xl px-5 py-2">{isEdit ? "Lưu thay đổi" : "Giao việc"}</Button>
      </AdminDialogFooter>
    </AdminDialog>
  );
}
