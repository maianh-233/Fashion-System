import { useEffect, useState } from "react";
import Button from "../../common/Button";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../common/AdminDialog";
import { AdminField, AdminFormSection } from "../common/AdminForm";

export default function TaskReportDialog({ open, task, employee, onClose, onSubmit }) {
  const [report, setReport] = useState({ progress: 0, status: "IN_PROGRESS", actualHours: 0, workDone: "", blockers: "" });

  useEffect(() => {
    if (!open || !task) return;
    setReport({ progress: task.progress || 0, status: task.status === "TODO" ? "IN_PROGRESS" : task.status, actualHours: task.actualHours || 0, workDone: "", blockers: "" });
  }, [open, task]);

  if (!task) return null;
  const update = (field, value) => setReport((current) => ({ ...current, [field]: value }));
  const submit = () => onSubmit?.({ ...report, reportedAt: new Date().toLocaleString("vi-VN"), reporterId: task.assigneeId });

  return (
    <AdminDialog open={open} onClose={onClose} size="md">
      <AdminDialogHeader title="Báo cáo tiến độ" description={`${task.code} • ${task.title}`} onClose={onClose} />
      <AdminDialogBody className="space-y-5">
        <div className="task-report-owner"><span>{employee?.name?.split(" ").slice(-2).map((part) => part[0]).join("")}</span><div><strong>{employee?.name}</strong><small>{employee?.position} • {employee?.department}</small></div></div>
        <AdminFormSection title="Kết quả thực hiện">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <AdminField as="select" label="Trạng thái" value={report.status} onChange={(event) => update("status", event.target.value)}>
              <option value="IN_PROGRESS">Đang thực hiện</option><option value="REVIEW">Chờ duyệt</option><option value="COMPLETED">Hoàn thành</option><option value="BLOCKED">Đang vướng mắc</option>
            </AdminField>
            <AdminField label="Tiến độ (%)" type="number" min="0" max="100" value={report.progress} onChange={(event) => update("progress", Math.min(100, Math.max(0, Number(event.target.value))))} />
            <AdminField label="Số giờ đã thực hiện" type="number" min="0" step="0.5" value={report.actualHours} onChange={(event) => update("actualHours", Number(event.target.value))} />
            <div className="task-progress-preview"><span>Tiến độ hiện tại</span><div><i style={{ width: `${report.progress}%` }} /></div><strong>{report.progress}%</strong></div>
            <AdminField as="textarea" rows="4" label="Công việc đã thực hiện *" value={report.workDone} onChange={(event) => update("workDone", event.target.value)} containerClassName="sm:col-span-2" placeholder="Mô tả kết quả, số liệu hoặc đầu ra đã hoàn thành..." />
            <AdminField as="textarea" rows="3" label="Khó khăn / hỗ trợ cần thiết" value={report.blockers} onChange={(event) => update("blockers", event.target.value)} containerClassName="sm:col-span-2" placeholder="Nêu rõ vấn đề đang cản trở tiến độ..." />
          </div>
        </AdminFormSection>
      </AdminDialogBody>
      <AdminDialogFooter>
        <Button type="button" variant="secondary" onClick={onClose} className="rounded-xl px-5 py-2">Hủy</Button>
        <Button type="button" variant="primary" disabled={!report.workDone.trim()} onClick={submit} className="rounded-xl px-5 py-2">Gửi báo cáo</Button>
      </AdminDialogFooter>
    </AdminDialog>
  );
}
