import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, CheckCircle2, ListChecks, Pencil, Play, RotateCcw, Send, XCircle } from "lucide-react";
import AdminCatalogPageHeader from "../../../components/admin/common/AdminCatalogPageHeader";
import {
  Badge, Empty, ErrorText, Loading, ReasonDialog, Section, btnDanger, btnGhost, btnPrimary, textarea, useLoader,
} from "../../../components/admin/recruitment/RecruitmentUi";
import { taskApi } from "../../../api/admin/taskApi";
import TaskFormDialog from "./TaskFormDialog";
import { HISTORY_LABEL, TASK_PRIORITY, TASK_STATUS, formatDateTime } from "./taskLogic";

/** Chi tiết công việc: checklist, bình luận, lịch sử và các thao tác theo vai trò của người xem. */
export default function TaskDetail() {
  const { id } = useParams();
  const { data, error, loading, setData } = useLoader((signal) => taskApi.detail(id, { signal }), [id]);
  const [dialog, setDialog] = useState(null);
  const [actionError, setActionError] = useState("");
  const [busy, setBusy] = useState(false);

  const run = async (action) => {
    setBusy(true);
    setActionError("");
    try {
      setData(await action());
    } catch (e) {
      setActionError(e.message);
    } finally {
      setBusy(false);
    }
  };
  const confirm = async (action) => {
    setData(await action());
    setDialog(null);
  };

  const back = <Link to="/admin/me/tasks" className="inline-flex items-center gap-1.5 text-sm text-zinc-400 hover:text-zinc-200"><ArrowLeft size={15} />Công việc của tôi</Link>;
  if (!data) {
    return <div className="admin-catalog-page space-y-5 text-zinc-100">{back}<ErrorText>{error}</ErrorText>{loading && <Loading />}</div>;
  }

  const { task, checklist, comments, history, batch, actions } = data;
  return (
    <div className="admin-catalog-page space-y-5 text-zinc-100 [&>*]:min-w-0">
      {back}
      <AdminCatalogPageHeader icon={ListChecks} eyebrow={task.code} title={task.title}
        description={`${task.assignerName || "Không rõ"} giao cho ${task.assigneeName}`}
        action={<Badge map={TASK_STATUS} value={task.status} />} />
      <ErrorText>{actionError}</ErrorText>

      {task.status === "IN_PROGRESS" && task.returnReason && (
        <p className="rounded-xl border border-amber-500/30 bg-amber-500/10 px-3 py-2 text-sm text-amber-200">Bị trả lại: {task.returnReason}</p>
      )}

      <div className="flex flex-wrap gap-2">
        {actions.start && <button type="button" className={btnGhost} disabled={busy} onClick={() => run(() => taskApi.start(id))}><Play size={15} />Bắt đầu</button>}
        {actions.complete && <button type="button" className={btnPrimary} disabled={busy} onClick={() => setDialog("complete")}><CheckCircle2 size={16} />Xác nhận hoàn thành</button>}
        {actions.edit && <button type="button" className={btnGhost} disabled={busy} onClick={() => setDialog("edit")}><Pencil size={15} />Sửa</button>}
        {actions.giveBack && <button type="button" className={btnGhost} disabled={busy} onClick={() => setDialog("return")}><RotateCcw size={15} />Trả lại</button>}
        {actions.cancel && <button type="button" className={btnDanger} disabled={busy} onClick={() => setDialog("cancel")}><XCircle size={15} />Hủy công việc</button>}
      </div>

      <div className="grid gap-5 xl:grid-cols-[3fr_2fr] [&>*]:min-w-0">
        <div className="space-y-5 [&>*]:min-w-0">
          <Section title="Nội dung">
            {task.description ? <p className="whitespace-pre-wrap break-words text-sm text-zinc-200">{task.description}</p> : <p className="text-sm text-zinc-500">Không có mô tả.</p>}
            {task.completionNote && <p className="mt-3 rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-3 py-2 text-sm text-emerald-200">Ghi chú kết quả: {task.completionNote}</p>}
          </Section>
          {checklist.length > 0 && (
            <Section title={`Việc con (${task.checklistDone}/${task.checklistTotal})`}>
              <ul className="space-y-1">
                {checklist.map((item) => (
                  <li key={item.id}>
                    <label className={`flex items-start gap-3 rounded-xl px-2 py-2 text-sm ${actions.toggle ? "cursor-pointer hover:bg-zinc-800/50" : ""}`}>
                      <input type="checkbox" className="mt-0.5 h-4 w-4 accent-amber-400" checked={item.done} disabled={!actions.toggle || busy}
                        onChange={(e) => run(() => taskApi.toggleItem(id, item.id, e.target.checked))} />
                      <span className={`break-words ${item.done ? "text-zinc-500 line-through" : "text-zinc-100"}`}>{item.content}</span>
                    </label>
                  </li>
                ))}
              </ul>
            </Section>
          )}
          <Comments comments={comments} canComment={actions.comment} onSend={(content) => confirm(() => taskApi.comment(id, content))} />
        </div>

        <div className="space-y-5 [&>*]:min-w-0">
          <Section title="Thông tin">
            <dl className="space-y-2.5 text-sm">
              <Info label="Người giao">{task.assignerName || "Không rõ"}</Info>
              <Info label="Người được giao">{task.assigneeName}</Info>
              <Info label="Cửa hàng">{task.storeName || "Toàn hệ thống"}</Info>
              <Info label="Ưu tiên"><Badge map={TASK_PRIORITY} value={task.priority} /></Info>
              <Info label="Hạn chót">
                {task.dueAt ? <span className={task.overdue ? "font-semibold text-rose-300" : ""}>{formatDateTime(task.dueAt)}{task.overdue ? " · Quá hạn" : ""}</span> : "Không có hạn"}
              </Info>
              {task.completedAt && <Info label="Hoàn thành lúc"><span className={task.late ? "text-rose-300" : ""}>{formatDateTime(task.completedAt)}{task.late ? " · Trễ hạn" : ""}</span></Info>}
              <Info label="Giao lúc">{formatDateTime(task.createdAt)}</Info>
              {task.returnCount > 0 && <Info label="Số lần trả lại">{task.returnCount}</Info>}
            </dl>
          </Section>
          {batch.length > 1 && (
            <Section title="Cùng đợt giao">
              <ul className="space-y-2">
                {batch.map((other) => (
                  <li key={other.id} className="flex items-center justify-between gap-3 text-sm">
                    <Link to={`/admin/me/tasks/${other.id}`} className={`min-w-0 truncate hover:text-amber-300 ${other.id === task.id ? "font-semibold text-amber-200" : "text-zinc-200"}`}>{other.assigneeName}</Link>
                    <Badge map={TASK_STATUS} value={other.status} />
                  </li>
                ))}
              </ul>
            </Section>
          )}
          <Section title="Lịch sử">
            <ol className="relative space-y-4 border-l border-zinc-800 pl-5">
              {history.map((h) => (
                <li key={h.id} className="relative">
                  <span className="absolute -left-[26px] top-1.5 h-2.5 w-2.5 rounded-full bg-amber-400" />
                  <p className="text-sm text-zinc-100">{HISTORY_LABEL[h.action] || h.action}</p>
                  {h.note && <p className="mt-0.5 break-words text-xs text-zinc-400">{h.note}</p>}
                  <p className="mt-0.5 text-xs text-zinc-500">{formatDateTime(h.createdAt)} · {h.actorName || "Hệ thống"}</p>
                </li>
              ))}
            </ol>
          </Section>
        </div>
      </div>

      {dialog === "edit" && <TaskFormDialog task={task} checklist={checklist} onClose={() => setDialog(null)} onSaved={(detail) => { setData(detail); setDialog(null); }} />}
      {dialog === "complete" && (
        <ReasonDialog title="Xác nhận hoàn thành" description={`${task.assignerName || "Người giao việc"} sẽ nhận được thông báo. Sau khi xác nhận, chỉ người giao mới mở lại được.`}
          label="Ghi chú kết quả" required={false} confirmText="Xác nhận hoàn thành" onClose={() => setDialog(null)}
          onConfirm={(note) => confirm(() => taskApi.complete(id, note || null))} />
      )}
      {dialog === "return" && (
        <ReasonDialog title="Trả lại công việc" description="Công việc quay về trạng thái Đang làm và người được giao sẽ nhận được lý do."
          confirmText="Trả lại" onClose={() => setDialog(null)} onConfirm={(reason) => confirm(() => taskApi.giveBack(id, reason))} />
      )}
      {dialog === "cancel" && (
        <ReasonDialog title="Hủy công việc" description="Người được giao sẽ nhận được thông báo. Không thể khôi phục công việc đã hủy."
          required={false} danger confirmText="Hủy công việc" onClose={() => setDialog(null)} onConfirm={(reason) => confirm(() => taskApi.cancel(id, reason || null))} />
      )}
    </div>
  );
}

function Info({ label, children }) {
  return (
    <div className="flex items-start justify-between gap-4">
      <dt className="shrink-0 text-zinc-400">{label}</dt>
      <dd className="min-w-0 break-words text-right text-zinc-100">{children}</dd>
    </div>
  );
}

function Comments({ comments, canComment, onSend }) {
  const [content, setContent] = useState("");
  const [error, setError] = useState("");
  const [sending, setSending] = useState(false);
  const submit = async (event) => {
    event.preventDefault();
    if (!content.trim()) return;
    setSending(true);
    setError("");
    try {
      await onSend(content.trim());
      setContent("");
    } catch (e) {
      setError(e.message);
    } finally {
      setSending(false);
    }
  };
  return (
    <Section title={`Bình luận (${comments.length})`}>
      {comments.length === 0 ? <Empty>Chưa có bình luận.</Empty> : (
        <ul className="mb-4 space-y-3">
          {comments.map((c) => (
            <li key={c.id} className="rounded-2xl border border-zinc-800 bg-zinc-950/60 px-3 py-2.5">
              <p className="text-xs text-zinc-400"><span className="font-semibold text-zinc-200">{c.authorName || "Không rõ"}</span> · {formatDateTime(c.createdAt)}</p>
              <p className="mt-1 whitespace-pre-wrap break-words text-sm text-zinc-100">{c.content}</p>
            </li>
          ))}
        </ul>
      )}
      {canComment && (
        <form onSubmit={submit} className="space-y-2">
          <textarea className={textarea} rows={2} maxLength={2000} value={content} onChange={(e) => setContent(e.target.value)} placeholder="Viết bình luận…" aria-label="Viết bình luận" />
          <ErrorText>{error}</ErrorText>
          <div className="flex justify-end">
            <button type="submit" className={btnPrimary} disabled={sending || !content.trim()}><Send size={15} />{sending ? "Đang gửi…" : "Gửi"}</button>
          </div>
        </form>
      )}
    </Section>
  );
}
