import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Check, ListChecks, MessageSquare, Plus } from "lucide-react";
import PageGuide from "../../../components/admin/common/PageGuide";
import AdminCatalogPageHeader from "../../../components/admin/common/AdminCatalogPageHeader";
import Pagination from "../../../components/common/Pagination";
import {
  Badge, Empty, ErrorText, Field, Loading, ReasonDialog, btnPrimary, card, control, useLoader,
} from "../../../components/admin/recruitment/RecruitmentUi";
import { taskApi } from "../../../api/admin/taskApi";
import TaskFormDialog from "../task/TaskFormDialog";
import { TASK_PRIORITY, TASK_STATUS, formatDateTime, groupByDue } from "../task/taskLogic";

const TABS = {
  open: { label: "Đang làm", params: { view: "ASSIGNED_TO_ME", state: "OPEN" }, empty: "Bạn không có việc nào cần làm." },
  closed: { label: "Đã xong", params: { view: "ASSIGNED_TO_ME", state: "CLOSED" }, empty: "Chưa có việc nào đã xong." },
  assigned: { label: "Tôi đã giao", params: { view: "ASSIGNED_BY_ME" }, empty: "Bạn chưa giao việc nào." },
};

/** Todolist cá nhân: việc được giao cho mình và việc mình đã giao cho người khác. */
export default function MyTasks() {
  const [params, setParams] = useSearchParams();
  const lookups = useLoader((signal) => taskApi.lookups({ signal }), []);
  const canAssign = Boolean(lookups.data?.canAssign);
  const requested = params.get("tab");
  const tab = TABS[requested] && (requested !== "assigned" || canAssign) ? requested : "open";
  const [priority, setPriority] = useState("");
  const [page, setPage] = useState(1);
  const [version, setVersion] = useState(0);
  const [creating, setCreating] = useState(false);
  const [completing, setCompleting] = useState(null);
  const { data, error, loading } = useLoader(
    (signal) => taskApi.mine({ ...TABS[tab].params, priority, page: page - 1, size: 30 }, { signal }), [tab, priority, page, version]);
  const rows = data?.content || [];

  const changeTab = (key) => {
    setPage(1);
    setParams(key === "open" ? {} : { tab: key }, { replace: true });
  };
  const visibleTabs = Object.keys(TABS).filter((key) => key !== "assigned" || canAssign);

  return (
    <div className="admin-catalog-page space-y-5 text-zinc-100 [&>*]:min-w-0">
      <AdminCatalogPageHeader icon={ListChecks} eyebrow="Cá nhân" title="Công việc của tôi"
        description="Việc cấp trên giao cho bạn: tick từng bước và xác nhận hoàn thành để người giao nhận được thông báo."
        action={canAssign ? <button type="button" className={btnPrimary} onClick={() => setCreating(true)}><Plus size={16} />Giao việc</button> : null} />
      <PageGuide id="myTasks" />

      <div className="flex flex-wrap items-end justify-between gap-3">
        <div className="flex flex-wrap gap-2" role="tablist">
          {visibleTabs.map((key) => (
            <button key={key} type="button" role="tab" aria-selected={tab === key} onClick={() => changeTab(key)}
              className={`rounded-xl border px-3 py-2 text-sm ${tab === key ? "border-amber-400 bg-amber-500/10 text-amber-200" : "border-zinc-700 text-zinc-400 hover:text-zinc-200"}`}>
              {TABS[key].label}
            </button>
          ))}
        </div>
        <Field label="Ưu tiên" className="w-full sm:w-44">
          <select className={control} value={priority} onChange={(e) => { setPriority(e.target.value); setPage(1); }}>
            <option value="">Tất cả</option>
            {Object.entries(TASK_PRIORITY).map(([value, p]) => <option key={value} value={value}>{p.label}</option>)}
          </select>
        </Field>
      </div>

      <ErrorText>{error}</ErrorText>
      {loading && !data ? <Loading /> : !rows.length ? <Empty>{TABS[tab].empty}</Empty> : tab === "open" ? (
        <div className="space-y-5">
          {groupByDue(rows).map((group) => (
            <section key={group.key} className="space-y-2">
              <h2 className={`text-sm font-semibold ${group.key === "OVERDUE" ? "text-rose-300" : "text-zinc-300"}`}>{group.label} ({group.tasks.length})</h2>
              <TaskList tasks={group.tasks} onTick={setCompleting} />
            </section>
          ))}
        </div>
      ) : <TaskList tasks={rows} showAssignee={tab === "assigned"} />}
      <Pagination currentPage={page} totalPages={data?.page?.totalPages || data?.totalPages || 1} onPageChange={setPage} />

      {creating && (
        <TaskFormDialog assignees={lookups.data.assignees} onClose={() => setCreating(false)}
          onSaved={() => { setCreating(false); setVersion((v) => v + 1); changeTab("assigned"); }} />
      )}
      {completing && (
        <ReasonDialog title="Xác nhận hoàn thành" description={`“${completing.title}” sẽ được báo cho ${completing.assignerName || "người giao việc"}. Sau khi xác nhận, chỉ người giao mới mở lại được.`}
          label="Ghi chú kết quả" required={false} confirmText="Xác nhận hoàn thành" onClose={() => setCompleting(null)}
          onConfirm={async (note) => { await taskApi.complete(completing.id, note || null); setCompleting(null); setVersion((v) => v + 1); }} />
      )}
    </div>
  );
}

function TaskList({ tasks, onTick, showAssignee = false }) {
  return (
    <ul className={`${card} divide-y divide-zinc-800 overflow-hidden`}>
      {tasks.map((task) => <TaskRow key={task.id} task={task} onTick={onTick} showAssignee={showAssignee} />)}
    </ul>
  );
}

function TaskRow({ task, onTick, showAssignee }) {
  const remaining = task.checklistTotal - task.checklistDone;
  const blocked = remaining > 0;
  return (
    <li className="flex items-start gap-3 px-4 py-3 hover:bg-zinc-800/40">
      {onTick && (
        <button type="button" disabled={blocked} onClick={() => onTick(task)}
          aria-label={blocked ? `Còn ${remaining} việc con chưa xong` : `Xác nhận hoàn thành ${task.title}`}
          title={blocked ? `Còn ${remaining} việc con chưa xong` : "Xác nhận hoàn thành"}
          className="group mt-0.5 grid h-6 w-6 shrink-0 place-items-center rounded-full border-2 border-zinc-600 text-transparent hover:border-amber-400 hover:text-amber-400 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:border-zinc-600 disabled:hover:text-transparent">
          <Check size={14} strokeWidth={3} />
        </button>
      )}
      <div className="min-w-0 flex-1">
        <Link to={`/admin/me/tasks/${task.id}`} className="block break-words text-sm font-medium text-zinc-100 hover:text-amber-300">{task.title}</Link>
        <p className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-zinc-400">
          <span>{showAssignee ? `Giao cho ${task.assigneeName}` : `Từ ${task.assignerName || "Không rõ"}`}</span>
          {task.dueAt && <span className={task.overdue ? "font-semibold text-rose-300" : ""}>Hạn {formatDateTime(task.dueAt)}</span>}
          {task.checklistTotal > 0 && <span>{task.checklistDone}/{task.checklistTotal} việc con</span>}
          {task.commentCount > 0 && <span className="inline-flex items-center gap-1"><MessageSquare size={12} />{task.commentCount}</span>}
          {task.returnReason && task.status === "IN_PROGRESS" && <span className="text-amber-300">Bị trả lại</span>}
        </p>
      </div>
      <div className="flex shrink-0 flex-col items-end gap-1 sm:flex-row sm:items-center">
        {task.priority !== "NORMAL" && <Badge map={TASK_PRIORITY} value={task.priority} />}
        {!onTick && <Badge map={TASK_STATUS} value={task.status} />}
      </div>
    </li>
  );
}
