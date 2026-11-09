export const TASK_STATUS = {
  TODO: { label: "Cần làm", tone: "zinc" },
  IN_PROGRESS: { label: "Đang làm", tone: "sky" },
  DONE: { label: "Hoàn thành", tone: "emerald" },
  CANCELLED: { label: "Đã hủy", tone: "rose" },
};

export const TASK_PRIORITY = {
  LOW: { label: "Thấp", tone: "zinc" },
  NORMAL: { label: "Bình thường", tone: "sky" },
  HIGH: { label: "Cao", tone: "amber" },
  URGENT: { label: "Khẩn", tone: "rose" },
};

export const HISTORY_LABEL = {
  CREATED: "Giao việc",
  UPDATED: "Sửa nội dung",
  STARTED: "Bắt đầu làm",
  COMPLETED: "Xác nhận hoàn thành",
  RETURNED: "Trả lại",
  CANCELLED: "Hủy công việc",
};

export const formatDateTime = (value) => (value ? new Date(value).toLocaleString("vi-VN", { dateStyle: "short", timeStyle: "short" }) : "");

/** Ngày theo giờ máy người dùng dạng yyyy-MM-dd, dùng cho ô chọn ngày. */
export function dateInput(date = new Date()) {
  const pad = (n) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

/** Giá trị từ API → giá trị ô datetime-local (bỏ phần giây). */
export const toDateTimeInput = (value) => (value ? String(value).slice(0, 16) : "");

const sameDay = (a, b) => a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();

export const DUE_GROUPS = [
  { key: "OVERDUE", label: "Quá hạn" },
  { key: "TODAY", label: "Hôm nay" },
  { key: "UPCOMING", label: "Sắp tới" },
  { key: "NONE", label: "Không có hạn" },
];

export function dueGroup(task, now = new Date()) {
  if (!task.dueAt) return "NONE";
  if (task.overdue) return "OVERDUE";
  return sameDay(new Date(task.dueAt), now) ? "TODAY" : "UPCOMING";
}

/** Nhóm danh sách việc theo hạn chót, bỏ các nhóm rỗng và giữ thứ tự sẵn có trong từng nhóm. */
export function groupByDue(tasks, now = new Date()) {
  return DUE_GROUPS
    .map((group) => ({ ...group, tasks: tasks.filter((task) => dueGroup(task, now) === group.key) }))
    .filter((group) => group.tasks.length > 0);
}

export function emptyTaskForm() {
  return { assigneeIds: [], title: "", description: "", priority: "NORMAL", dueAt: "", checklist: [] };
}

export function taskToForm(task, checklist) {
  return {
    assigneeIds: [],
    title: task.title,
    description: task.description || "",
    priority: task.priority,
    dueAt: toDateTimeInput(task.dueAt),
    checklist: checklist.map((item) => ({ id: item.id, content: item.content })),
  };
}

/** Trả về thông báo lỗi đầu tiên, hoặc chuỗi rỗng khi hợp lệ. */
export function validateTaskForm(form, { creating }) {
  if (creating && form.assigneeIds.length === 0) return "Vui lòng chọn người nhận việc";
  if (!form.title.trim()) return "Vui lòng nhập tên công việc";
  if (form.checklist.some((item) => !item.content.trim())) return "Việc con không được để trống";
  return "";
}

export function taskPayload(form, { creating }) {
  const body = {
    title: form.title.trim(),
    description: form.description.trim() || null,
    priority: form.priority,
    dueAt: form.dueAt || null,
    checklist: form.checklist.map((item) => ({ id: item.id || null, content: item.content.trim() })),
  };
  return creating ? { ...body, assigneeIds: form.assigneeIds } : body;
}
