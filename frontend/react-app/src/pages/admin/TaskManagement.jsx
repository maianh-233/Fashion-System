import { useMemo, useState } from "react";
import {
  AlertTriangle,
  BarChart3,
  CalendarClock,
  CheckCircle2,
  ClipboardCheck,
  Clock3,
  Eye,
  FileText,
  Pencil,
  Plus,
  Search,
  Target,
  Users,
} from "lucide-react";
import Button from "../../components/common/Button";
import AdminDetailDialog from "../../components/admin/common/AdminDetailDialog";
import TaskAssignmentDialog from "../../components/admin/Task/TaskAssignmentDialog";
import TaskReportDialog from "../../components/admin/Task/TaskReportDialog";

const manager = { id: "m1", name: "Nguyễn Văn An", position: "Trưởng phòng vận hành" };

const employees = [
  { id: "e1", name: "Trần Thị Linh", position: "Nhân viên bán hàng", department: "Kinh doanh", baseSalary: 10500000 },
  { id: "e2", name: "Lê Minh Quân", position: "Nhân viên kho", department: "Kho vận", baseSalary: 11000000 },
  { id: "e3", name: "Hoàng Thị Mai", position: "Chuyên viên CSKH", department: "Chăm sóc khách hàng", baseSalary: 10000000 },
  { id: "e4", name: "Phạm Ngọc Bích", position: "Kế toán viên", department: "Kế toán", baseSalary: 12000000 },
];

const initialTasks = [
  { id: "t1", code: "TASK-2026-001", title: "Đối soát đơn hàng tuần", description: "Đối soát trạng thái và doanh thu đơn hàng, ghi nhận các trường hợp sai lệch.", managerId: "m1", assigneeId: "e1", priority: "HIGH", status: "COMPLETED", startDate: "2026-08-03", dueDate: "2026-08-07", completedAt: "2026-08-07", estimatedHours: 12, actualHours: 11, progress: 100, qualityScore: 92, bonus: 400000, performanceWeight: 1.5, reports: [{ reportedAt: "07/08/2026 16:30", workDone: "Đã đối soát 326 đơn, xử lý xong 5 sai lệch thanh toán.", blockers: "", progress: 100 }] },
  { id: "t2", code: "TASK-2026-002", title: "Kiểm kê kho giữa tháng", description: "Kiểm kê tồn kho thực tế và lập biên bản chênh lệch.", managerId: "m1", assigneeId: "e2", priority: "URGENT", status: "IN_PROGRESS", startDate: "2026-08-11", dueDate: "2026-08-17", estimatedHours: 20, actualHours: 13, progress: 65, qualityScore: 86, bonus: 600000, performanceWeight: 2, reports: [{ reportedAt: "14/08/2026 17:15", workDone: "Đã kiểm 8/12 khu vực kho, phát hiện 3 mã lệch tồn.", blockers: "Cần xác nhận chứng từ nhập của ca tối.", progress: 65 }] },
  { id: "t3", code: "TASK-2026-003", title: "Khảo sát mức độ hài lòng", description: "Liên hệ nhóm khách hàng mua trong tháng và tổng hợp phản hồi.", managerId: "m1", assigneeId: "e3", priority: "MEDIUM", status: "REVIEW", startDate: "2026-08-05", dueDate: "2026-08-14", estimatedHours: 16, actualHours: 17, progress: 100, qualityScore: 89, bonus: 350000, performanceWeight: 1.5, reports: [{ reportedAt: "14/08/2026 15:40", workDone: "Hoàn thành 82 cuộc khảo sát và báo cáo nhóm vấn đề nổi bật.", blockers: "", progress: 100 }] },
  { id: "t4", code: "TASK-2026-004", title: "Tổng hợp chi phí vận hành", description: "Tập hợp hóa đơn, phân loại và tổng hợp chi phí vận hành tháng.", managerId: "m1", assigneeId: "e4", priority: "HIGH", status: "IN_PROGRESS", startDate: "2026-08-10", dueDate: "2026-08-20", estimatedHours: 18, actualHours: 8, progress: 45, qualityScore: 90, bonus: 500000, performanceWeight: 2, reports: [] },
  { id: "t5", code: "TASK-2026-005", title: "Cập nhật kịch bản tư vấn", description: "Cập nhật kịch bản tư vấn theo bộ sưu tập mới.", managerId: "m1", assigneeId: "e1", priority: "MEDIUM", status: "TODO", startDate: "2026-08-16", dueDate: "2026-08-21", estimatedHours: 8, actualHours: 0, progress: 0, qualityScore: null, bonus: 250000, performanceWeight: 1, reports: [] },
  { id: "t6", code: "TASK-2026-006", title: "Xử lý yêu cầu đổi trả tồn đọng", description: "Rà soát và xử lý toàn bộ phiếu đổi trả quá 48 giờ.", managerId: "m1", assigneeId: "e3", priority: "URGENT", status: "BLOCKED", startDate: "2026-08-08", dueDate: "2026-08-13", estimatedHours: 10, actualHours: 7, progress: 55, qualityScore: 82, bonus: 300000, performanceWeight: 1.5, reports: [{ reportedAt: "13/08/2026 10:20", workDone: "Đã xử lý 11/20 yêu cầu.", blockers: "Chờ kho xác nhận tình trạng 9 sản phẩm.", progress: 55 }] },
  { id: "t7", code: "TASK-2026-007", title: "Sắp xếp lại khu vực hàng mới", description: "Phân khu và gắn nhãn toàn bộ hàng của bộ sưu tập mới.", managerId: "m1", assigneeId: "e2", priority: "LOW", status: "COMPLETED", startDate: "2026-08-01", dueDate: "2026-08-05", completedAt: "2026-08-04", estimatedHours: 9, actualHours: 8, progress: 100, qualityScore: 95, bonus: 250000, performanceWeight: 1, reports: [] },
  { id: "t8", code: "TASK-2026-008", title: "Đối chiếu công nợ nhà cung cấp", description: "Đối chiếu số dư công nợ và lập danh sách cần xác minh.", managerId: "m1", assigneeId: "e4", priority: "MEDIUM", status: "COMPLETED", startDate: "2026-08-01", dueDate: "2026-08-09", completedAt: "2026-08-10", estimatedHours: 14, actualHours: 15, progress: 100, qualityScore: 88, bonus: 300000, performanceWeight: 1.5, reports: [] },
];

const statusMap = {
  TODO: { label: "Chưa bắt đầu", className: "task-status--todo" },
  IN_PROGRESS: { label: "Đang thực hiện", className: "task-status--progress" },
  REVIEW: { label: "Chờ duyệt", className: "task-status--review" },
  COMPLETED: { label: "Hoàn thành", className: "task-status--completed" },
  BLOCKED: { label: "Vướng mắc", className: "task-status--blocked" },
};

const priorityMap = { LOW: "Thấp", MEDIUM: "Trung bình", HIGH: "Cao", URGENT: "Khẩn cấp" };
const currency = (value) => new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(value || 0);
const formatDate = (value) => value ? new Intl.DateTimeFormat("vi-VN").format(new Date(`${value}T00:00:00`)) : "—";

function StatusBadge({ status }) {
  const item = statusMap[status] || statusMap.TODO;
  return <span className={`task-status ${item.className}`}>{item.label}</span>;
}

export default function TaskManagement() {
  const [tasks, setTasks] = useState(initialTasks);
  const [activeTab, setActiveTab] = useState("tasks");
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [assigneeFilter, setAssigneeFilter] = useState("ALL");
  const [priorityFilter, setPriorityFilter] = useState("ALL");
  const [assignmentTask, setAssignmentTask] = useState(undefined);
  const [assignmentOpen, setAssignmentOpen] = useState(false);
  const [reportTask, setReportTask] = useState(null);
  const [detailTask, setDetailTask] = useState(null);

  const employeeById = useMemo(() => Object.fromEntries(employees.map((employee) => [employee.id, employee])), []);
  const today = new Date("2026-08-15T00:00:00");
  const isOverdue = (task) => task.status !== "COMPLETED" && task.dueDate && new Date(`${task.dueDate}T00:00:00`) < today;

  const filteredTasks = useMemo(() => tasks.filter((task) => {
    const term = search.trim().toLocaleLowerCase("vi");
    const employee = employeeById[task.assigneeId];
    const matchesSearch = !term || `${task.code} ${task.title} ${employee?.name || ""}`.toLocaleLowerCase("vi").includes(term);
    return matchesSearch && (statusFilter === "ALL" || task.status === statusFilter) && (assigneeFilter === "ALL" || task.assigneeId === assigneeFilter) && (priorityFilter === "ALL" || task.priority === priorityFilter);
  }), [tasks, search, statusFilter, assigneeFilter, priorityFilter, employeeById]);

  const performance = useMemo(() => employees.map((employee) => {
    const ownTasks = tasks.filter((task) => task.assigneeId === employee.id);
    const completed = ownTasks.filter((task) => task.status === "COMPLETED");
    const totalWeight = ownTasks.reduce((sum, task) => sum + (task.performanceWeight || 1), 0) || 1;
    const progress = Math.round(ownTasks.reduce((sum, task) => sum + task.progress * (task.performanceWeight || 1), 0) / totalWeight);
    const qualityTasks = ownTasks.filter((task) => Number.isFinite(task.qualityScore));
    const quality = qualityTasks.length ? Math.round(qualityTasks.reduce((sum, task) => sum + task.qualityScore, 0) / qualityTasks.length) : 80;
    const datedCompleted = completed.filter((task) => task.completedAt && task.dueDate);
    const onTime = datedCompleted.length ? Math.round(datedCompleted.filter((task) => task.completedAt <= task.dueDate).length / datedCompleted.length * 100) : 100;
    const score = Math.round(progress * 0.5 + quality * 0.3 + onTime * 0.2);
    const taskBonus = Math.round(completed.reduce((sum, task) => sum + task.bonus * ((task.qualityScore || 80) / 100), 0));
    return { ...employee, assigned: ownTasks.length, completed: completed.length, progress, quality, onTime, score, hours: ownTasks.reduce((sum, task) => sum + (task.actualHours || 0), 0), taskBonus, estimatedPay: employee.baseSalary + taskBonus };
  }), [tasks]);

  const metrics = {
    active: tasks.filter((task) => !["COMPLETED"].includes(task.status)).length,
    overdue: tasks.filter(isOverdue).length,
    review: tasks.filter((task) => task.status === "REVIEW").length,
    completion: Math.round(tasks.filter((task) => task.status === "COMPLETED").length / tasks.length * 100),
  };

  const openCreate = () => { setAssignmentTask(undefined); setAssignmentOpen(true); };
  const openEdit = (task) => { setAssignmentTask(task); setAssignmentOpen(true); };
  const saveTask = (form) => {
    if (form.id) setTasks((current) => current.map((task) => task.id === form.id ? { ...task, ...form } : task));
    else {
      const number = String(tasks.length + 1).padStart(3, "0");
      setTasks((current) => [{ ...form, id: `t${Date.now()}`, code: `TASK-2026-${number}`, actualHours: 0, progress: 0, reports: [] }, ...current]);
    }
    setAssignmentOpen(false);
  };
  const submitReport = (report) => {
    setTasks((current) => current.map((task) => task.id === reportTask.id ? { ...task, ...report, completedAt: report.status === "COMPLETED" ? new Date().toISOString().slice(0, 10) : task.completedAt, reports: [...(task.reports || []), report] } : task));
    setReportTask(null);
  };

  return (
    <div className="task-page">
      <div className="task-page__header">
        <div><p className="task-page__eyebrow"><ClipboardCheck size={15} /> Quản trị nhân sự</p><h1>Công việc & hiệu suất</h1><p>{manager.name} · {manager.position}</p></div>
        <Button variant="primary" onClick={openCreate} className="rounded-xl px-4 py-2.5"><Plus size={18} /> Giao việc mới</Button>
      </div>

      <div className="task-kpis">
        <article className="task-kpi"><span><Target size={20} /></span><div><small>Việc đang mở</small><strong>{metrics.active}</strong></div></article>
        <article className="task-kpi task-kpi--danger"><span><AlertTriangle size={20} /></span><div><small>Đã quá hạn</small><strong>{metrics.overdue}</strong></div></article>
        <article className="task-kpi task-kpi--review"><span><Clock3 size={20} /></span><div><small>Chờ trưởng nhóm duyệt</small><strong>{metrics.review}</strong></div></article>
        <article className="task-kpi task-kpi--success"><span><CheckCircle2 size={20} /></span><div><small>Tỷ lệ hoàn thành</small><strong>{metrics.completion}%</strong></div></article>
      </div>

      <div className="task-tabs" role="tablist">
        <Button variant="ghost" className={`task-tab ${activeTab === "tasks" ? "is-active" : ""}`} onClick={() => setActiveTab("tasks")}><ClipboardCheck size={17} /> Danh sách công việc</Button>
        <Button variant="ghost" className={`task-tab ${activeTab === "performance" ? "is-active" : ""}`} onClick={() => setActiveTab("performance")}><BarChart3 size={17} /> Hiệu suất & thu nhập</Button>
      </div>

      {activeTab === "tasks" ? (
        <section className="task-panel">
          <div className="task-filters">
            <label className="task-search"><Search size={17} /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Tìm mã, công việc, nhân viên..." /></label>
            <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}><option value="ALL">Tất cả trạng thái</option>{Object.entries(statusMap).map(([key, item]) => <option key={key} value={key}>{item.label}</option>)}</select>
            <select value={assigneeFilter} onChange={(event) => setAssigneeFilter(event.target.value)}><option value="ALL">Tất cả nhân viên</option>{employees.map((employee) => <option key={employee.id} value={employee.id}>{employee.name}</option>)}</select>
            <select value={priorityFilter} onChange={(event) => setPriorityFilter(event.target.value)}><option value="ALL">Tất cả ưu tiên</option>{Object.entries(priorityMap).map(([key, label]) => <option key={key} value={key}>{label}</option>)}</select>
          </div>
          <div className="task-table-wrap">
            <table className="task-table"><thead><tr><th>Công việc</th><th>Nhân viên</th><th>Thời hạn</th><th>Tiến độ</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
              <tbody>{filteredTasks.map((task) => { const employee = employeeById[task.assigneeId]; return <tr key={task.id}>
                <td data-label="Công việc"><div className="task-title"><small>{task.code} · <span className={`task-priority task-priority--${task.priority.toLowerCase()}`}>{priorityMap[task.priority]}</span></small><strong>{task.title}</strong></div></td>
                <td data-label="Nhân viên"><div className="task-person"><span>{employee?.name?.split(" ").slice(-2).map((part) => part[0]).join("")}</span><div><strong>{employee?.name}</strong><small>{employee?.department}</small></div></div></td>
                <td data-label="Thời hạn"><div className={`task-deadline ${isOverdue(task) ? "is-overdue" : ""}`}><CalendarClock size={15} /><span>{formatDate(task.dueDate)}{isOverdue(task) && <small>Quá hạn</small>}</span></div></td>
                <td data-label="Tiến độ"><div className="task-progress"><div><i style={{ width: `${task.progress}%` }} /></div><strong>{task.progress}%</strong></div></td>
                <td data-label="Trạng thái"><StatusBadge status={task.status} /></td>
                <td data-label="Thao tác"><div className="task-actions"><Button variant="ghost" title="Xem chi tiết" onClick={() => setDetailTask(task)}><Eye size={17} /></Button><Button variant="ghost" title="Chỉnh sửa" onClick={() => openEdit(task)}><Pencil size={17} /></Button><Button variant="ghost" title="Báo cáo tiến độ" onClick={() => setReportTask(task)}><FileText size={17} /></Button></div></td>
              </tr>; })}</tbody>
            </table>
            {!filteredTasks.length && <div className="task-empty">Không tìm thấy công việc phù hợp.</div>}
          </div>
        </section>
      ) : (
        <section className="task-panel">
          <div className="performance-heading"><div><h2>Hiệu suất đội ngũ tháng 08/2026</h2><p>Điểm hiệu suất = 50% tiến độ + 30% chất lượng + 20% đúng hạn.</p></div><span><Users size={18} /> {employees.length} nhân viên dưới quyền</span></div>
          <div className="performance-grid">{performance.map((item) => <article className="performance-card" key={item.id}>
            <div className="performance-card__head"><div className="task-person"><span>{item.name.split(" ").slice(-2).map((part) => part[0]).join("")}</span><div><strong>{item.name}</strong><small>{item.position} · {item.department}</small></div></div><div className={`performance-score ${item.score >= 85 ? "is-good" : item.score < 70 ? "is-low" : ""}`}><strong>{item.score}</strong><small>điểm</small></div></div>
            <div className="performance-stats"><div><small>Hoàn thành</small><strong>{item.completed}/{item.assigned} việc</strong></div><div><small>Tiến độ TB</small><strong>{item.progress}%</strong></div><div><small>Đúng hạn</small><strong>{item.onTime}%</strong></div><div><small>Giờ thực tế</small><strong>{item.hours} giờ</strong></div></div>
            <div className="performance-pay"><div><small>Lương cơ bản</small><span>{currency(item.baseSalary)}</span></div><div><small>Thưởng theo việc</small><span className="is-bonus">+ {currency(item.taskBonus)}</span></div><div className="performance-pay__total"><strong>Thu nhập dự kiến</strong><strong>{currency(item.estimatedPay)}</strong></div></div>
          </article>)}</div>
          <p className="performance-note">* Thu nhập trên trang này là số dự kiến từ lương cơ bản và thưởng công việc. Bảng lương chính thức cần kết hợp chấm công, hợp đồng, phụ cấp, thuế và các khoản khấu trừ.</p>
        </section>
      )}

      <TaskAssignmentDialog open={assignmentOpen} task={assignmentTask} employees={employees} managerId={manager.id} onClose={() => setAssignmentOpen(false)} onSave={saveTask} />
      <TaskReportDialog open={Boolean(reportTask)} task={reportTask} employee={reportTask ? employeeById[reportTask.assigneeId] : null} onClose={() => setReportTask(null)} onSubmit={submitReport} />
      <AdminDetailDialog open={Boolean(detailTask)} title={detailTask?.title} description={detailTask ? `${detailTask.code} · Giao bởi ${manager.name}` : ""} onClose={() => setDetailTask(null)} size="lg" showFooter>
        {detailTask && <div className="task-detail"><div className="task-detail__summary"><div><small>Nhân viên</small><strong>{employeeById[detailTask.assigneeId]?.name}</strong></div><div><small>Thời hạn</small><strong>{formatDate(detailTask.startDate)} – {formatDate(detailTask.dueDate)}</strong></div><div><small>Trạng thái</small><StatusBadge status={detailTask.status} /></div><div><small>Tiến độ</small><strong>{detailTask.progress}% · {detailTask.actualHours}/{detailTask.estimatedHours} giờ</strong></div></div><div className="task-detail__description"><h3>Yêu cầu công việc</h3><p>{detailTask.description || "Chưa có mô tả."}</p></div><div className="task-report-history"><h3>Lịch sử báo cáo</h3>{detailTask.reports?.length ? detailTask.reports.slice().reverse().map((report, index) => <article key={`${report.reportedAt}-${index}`}><div><strong>{report.progress}% hoàn thành</strong><time>{report.reportedAt}</time></div><p>{report.workDone}</p>{report.blockers && <small><AlertTriangle size={14} /> {report.blockers}</small>}</article>) : <p className="task-empty">Nhân viên chưa gửi báo cáo tiến độ.</p>}</div></div>}
      </AdminDetailDialog>
    </div>
  );
}
