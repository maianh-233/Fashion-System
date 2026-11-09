import { useState } from "react";
import { Plus, X } from "lucide-react";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../../components/admin/common/AdminDialog";
import { ErrorText, Field, UserPicker, btnGhost, btnPrimary, control, textarea } from "../../../components/admin/recruitment/RecruitmentUi";
import { taskApi } from "../../../api/admin/taskApi";
import { TASK_PRIORITY, emptyTaskForm, taskPayload, taskToForm, validateTaskForm } from "./taskLogic";

/** Giao việc mới (chọn nhiều người) hoặc sửa một công việc khi truyền {@code task}. */
export default function TaskFormDialog({ assignees = [], task, checklist = [], onClose, onSaved }) {
  const creating = !task;
  const [form, setForm] = useState(() => (creating ? emptyTaskForm() : taskToForm(task, checklist)));
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const set = (key) => (event) => setForm({ ...form, [key]: event.target.value });
  const setItem = (index, content) => setForm({ ...form, checklist: form.checklist.map((item, i) => (i === index ? { ...item, content } : item)) });

  const submit = async (event) => {
    event.preventDefault();
    const problem = validateTaskForm(form, { creating });
    if (problem) return setError(problem);
    setSaving(true);
    setError("");
    try {
      const body = taskPayload(form, { creating });
      onSaved(creating ? await taskApi.create(body) : await taskApi.update(task.id, body));
    } catch (e) {
      setError(e.message);
      setSaving(false);
    }
  };

  return (
    <AdminDialog onClose={onClose}>
      <form onSubmit={submit}>
        <AdminDialogHeader title={creating ? "Giao việc" : `Sửa công việc ${task.code}`}
          description={creating ? "Chọn nhiều người thì mỗi người nhận một việc riêng và tự xác nhận hoàn thành." : "Người được giao sẽ nhận thông báo về thay đổi."} onClose={onClose} />
        <AdminDialogBody className="space-y-4">
          {creating && (
            <Field label="Giao cho" required>
              <UserPicker users={assignees} value={form.assigneeIds} onChange={(assigneeIds) => setForm({ ...form, assigneeIds })} />
            </Field>
          )}
          <Field label="Tên công việc" required>
            <input className={control} value={form.title} maxLength={200} onChange={set("title")} placeholder="Ví dụ: Sắp xếp lại kệ hàng mới về" />
          </Field>
          <Field label="Mô tả">
            <textarea className={textarea} rows={3} value={form.description} maxLength={5000} onChange={set("description")} />
          </Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Mức ưu tiên">
              <select className={control} value={form.priority} onChange={set("priority")}>
                {Object.entries(TASK_PRIORITY).map(([value, p]) => <option key={value} value={value}>{p.label}</option>)}
              </select>
            </Field>
            <Field label="Hạn chót" hint="Bỏ trống nếu không có hạn">
              <input type="datetime-local" className={control} value={form.dueAt} onChange={set("dueAt")} />
            </Field>
          </div>
          <div className="space-y-2">
            <p className="text-xs font-medium text-zinc-400">Danh sách việc con</p>
            {form.checklist.map((item, index) => (
              <div key={item.id || index} className="flex gap-2">
                <input className={control} value={item.content} maxLength={500} aria-label={`Việc con ${index + 1}`} onChange={(e) => setItem(index, e.target.value)} />
                <button type="button" className={btnGhost} aria-label="Xóa việc con"
                  onClick={() => setForm({ ...form, checklist: form.checklist.filter((_, i) => i !== index) })}><X size={15} /></button>
              </div>
            ))}
            {form.checklist.length < 50 && (
              <button type="button" className={btnGhost} onClick={() => setForm({ ...form, checklist: [...form.checklist, { content: "" }] })}>
                <Plus size={15} />Thêm việc con
              </button>
            )}
          </div>
          <ErrorText>{error}</ErrorText>
        </AdminDialogBody>
        <AdminDialogFooter>
          <button type="button" className={btnGhost} onClick={onClose}>Hủy</button>
          <button type="submit" className={btnPrimary} disabled={saving}>{saving ? "Đang lưu…" : creating ? "Giao việc" : "Lưu thay đổi"}</button>
        </AdminDialogFooter>
      </form>
    </AdminDialog>
  );
}
