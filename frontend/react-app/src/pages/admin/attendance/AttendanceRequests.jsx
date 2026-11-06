import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import { Check, Search, X } from "lucide-react";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../../components/admin/common/AdminDialog";
import Pagination from "../../../components/common/Pagination";
import { attendanceApi } from "../../../api/admin/attendanceApi";
import {
  Badge, Empty, ErrorText, Field, Loading, ReasonDialog, Section, btnDanger, btnGhost, btnPrimary, card, control, textarea, useLoader,
} from "../../../components/admin/recruitment/RecruitmentUi";
import { useAttendance } from "./AttendanceLayout";
import { EXCUSE_TYPES, OVERTIME_TYPES, REQUEST_STATUS, daysText, formatDate, formatDateTime, formatTime } from "./attendanceLogic";

const KINDS = [
  { value: "excuses", label: "Giải trình", need: "manage" },
  { value: "leaves", label: "Nghỉ phép", need: "approveLeave" },
  { value: "overtime", label: "Tăng ca", need: "manage" },
];

export default function AttendanceRequests() {
  const { can, stores, scopeStoreId } = useAttendance();
  const [params, setParams] = useSearchParams();
  const kind = KINDS.some((k) => k.value === params.get("kind")) ? params.get("kind") : "excuses";
  const [filter, setFilter] = useState({ status: "PENDING", storeId: "", keyword: "" });
  const [keyword, setKeyword] = useState("");
  const [page, setPage] = useState(1);
  const [approving, setApproving] = useState(null);
  const [rejecting, setRejecting] = useState(null);
  const { data, error, loading, reload } = useLoader(
    (signal) => attendanceApi.requests(kind, { ...filter, page: page - 1, size: 20 }, { signal }), [kind, JSON.stringify(filter), page]);
  const canDecide = Boolean(can[KINDS.find((k) => k.value === kind).need]);
  const update = (patch) => { setFilter((f) => ({ ...f, ...patch })); setPage(1); };

  return (
    <div className="space-y-4">
      <div className="flex gap-1 rounded-2xl border border-zinc-800 bg-zinc-900 p-1">
        {KINDS.map((k) => (
          <button key={k.value} type="button" onClick={() => { setParams({ kind: k.value }); setPage(1); }}
            className={`rounded-xl px-4 py-2 text-sm ${kind === k.value ? "bg-zinc-800 font-semibold text-amber-300" : "text-zinc-300 hover:text-white"}`}>
            {k.label}
          </button>
        ))}
      </div>
      <Section>
        <form className="grid gap-3 md:grid-cols-4" onSubmit={(e) => { e.preventDefault(); update({ keyword: keyword.trim() }); }}>
          <Field label="Trạng thái">
            <select className={control} value={filter.status} onChange={(e) => update({ status: e.target.value })}>
              <option value="">Tất cả</option>
              {Object.entries(REQUEST_STATUS).map(([v, s]) => <option key={v} value={v}>{s.label}</option>)}
            </select>
          </Field>
          <Field label="Cửa hàng">
            <select className={control} value={filter.storeId} onChange={(e) => update({ storeId: e.target.value })} disabled={Boolean(scopeStoreId)}>
              {!scopeStoreId && <option value="">Tất cả</option>}
              {stores.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </Field>
          <Field label="Tìm kiếm" className="md:col-span-2">
            <div className="flex gap-2">
              <input className={control} placeholder="Mã đơn, mã hoặc tên nhân viên" value={keyword} onChange={(e) => setKeyword(e.target.value)} />
              <button type="submit" className={btnGhost} aria-label="Tìm"><Search size={15} /></button>
            </div>
          </Field>
        </form>
      </Section>
      {!canDecide && <p className="text-sm text-zinc-400">Bạn chỉ có quyền xem các đơn này.</p>}
      <ErrorText>{error}</ErrorText>
      <div className={`${card} overflow-x-auto`}>
        <table className="min-w-full text-sm">
          <thead className="border-b border-zinc-800 text-left text-xs uppercase tracking-wide text-zinc-400">
            <tr><th className="px-4 py-3">Mã đơn</th><th className="px-4 py-3">Nhân viên</th><th className="px-4 py-3">Nội dung</th>
              <th className="px-4 py-3">Lý do</th><th className="px-4 py-3">Trạng thái</th><th className="px-4 py-3 text-right">Xử lý</th></tr>
          </thead>
          <tbody className="divide-y divide-zinc-800">
            {loading && !data && <tr><td colSpan="6"><Loading /></td></tr>}
            {data?.content?.length === 0 && <tr><td colSpan="6"><Empty>Không có đơn nào.</Empty></td></tr>}
            {data?.content?.map((r) => (
              <tr key={r.id} className="align-top">
                <td className="whitespace-nowrap px-4 py-3"><div className="font-medium">{r.code}</div><div className="text-xs text-zinc-500">{formatDateTime(r.createdAt)}</div></td>
                <td className="px-4 py-3"><div className="font-medium">{r.employeeName}</div><div className="text-xs text-zinc-500">{r.employeeCode}{r.storeName ? ` · ${r.storeName}` : " · Global"}</div></td>
                <td className="px-4 py-3 text-zinc-200"><Content kind={kind} r={r} /></td>
                <td className="max-w-xs px-4 py-3 text-zinc-300">
                  {r.reason}
                  {r.rejectionReason && <div className="mt-1 text-xs text-rose-300">Từ chối: {r.rejectionReason}</div>}
                  {r.approverNote && <div className="mt-1 text-xs text-zinc-500">Ghi chú: {r.approverNote}</div>}
                </td>
                <td className="px-4 py-3"><Badge map={REQUEST_STATUS} value={r.status} />{r.decidedByName && <div className="mt-1 text-xs text-zinc-500">{r.decidedByName}</div>}</td>
                <td className="whitespace-nowrap px-4 py-3 text-right">
                  {canDecide && r.status === "PENDING" && (
                    <div className="flex justify-end gap-2">
                      <button type="button" className={btnPrimary} onClick={() => setApproving(r)}><Check size={14} />Duyệt</button>
                      <button type="button" className={btnDanger} onClick={() => setRejecting(r)}><X size={14} />Từ chối</button>
                    </div>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Pagination currentPage={page} totalPages={data?.totalPages || 1} onPageChange={setPage} />

      {approving && <ApproveDialog kind={kind} request={approving} onClose={() => setApproving(null)} onDone={() => { setApproving(null); reload(); }} />}
      {rejecting && (
        <ReasonDialog title={`Từ chối đơn ${rejecting.code}`} description={rejecting.employeeName} label="Lý do từ chối" danger confirmText="Từ chối"
          onClose={() => setRejecting(null)}
          onConfirm={async (reason) => { await attendanceApi.reject(kind, rejecting.id, reason); setRejecting(null); reload(); }} />
      )}
    </div>
  );
}

function Content({ kind, r }) {
  if (kind === "excuses") return (
    <>
      <div>{EXCUSE_TYPES.find((t) => t.value === r.type)?.label} · {formatDate(r.date)}</div>
      <div className="text-xs text-zinc-400">
        {r.proposedCheckIn && `Vào ${formatTime(r.proposedCheckIn)} `}{r.proposedCheckOut && `Ra ${formatTime(r.proposedCheckOut)}`}
      </div>
    </>
  );
  if (kind === "leaves") return (
    <>
      <div>{r.leaveTypeName} <span className={`text-xs ${r.paid ? "text-emerald-300" : "text-zinc-400"}`}>({r.paid ? "có lương" : "không lương"})</span></div>
      <div className="text-xs text-zinc-400">
        {formatDate(r.startDate)}{r.endDate !== r.startDate && ` – ${formatDate(r.endDate)}`} · {daysText(r.totalDays)} ngày
        {r.halfDayStart && " · nửa ngày đầu"}{r.halfDayEnd && " · nửa ngày cuối"}
      </div>
    </>
  );
  return (
    <>
      <div>{formatDate(r.workDate)} · {OVERTIME_TYPES[r.type]}</div>
      <div className="text-xs text-zinc-400">Đăng ký {r.hours} giờ{r.approvedHours != null && ` · duyệt ${r.approvedHours} giờ`}</div>
    </>
  );
}

function ApproveDialog({ kind, request, onClose, onDone }) {
  const [note, setNote] = useState("");
  const [hours, setHours] = useState(request.hours ?? "");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      const body = { note: note.trim() || null };
      if (kind === "overtime") body.approvedHours = Number(hours);
      await attendanceApi.approve(kind, request.id, body);
      onDone();
    } catch (e) {
      setError(e.message);
      setSaving(false);
    }
  };
  return (
    <AdminDialog onClose={onClose}>
      <form onSubmit={submit}>
        <AdminDialogHeader title={`Duyệt đơn ${request.code}`} description={request.employeeName} onClose={onClose} />
        <AdminDialogBody className="space-y-3">
          {kind === "excuses" && <p className="text-sm text-zinc-300">Bảng công ngày {formatDate(request.date)} sẽ được cập nhật theo giờ đề xuất và lưu lịch sử điều chỉnh.</p>}
          {kind === "overtime" && (
            <Field label="Số giờ duyệt" hint={`Tối đa ${request.hours} giờ, bước 0.25`}>
              <input type="number" className={control} min="0.5" max={request.hours} step="0.25" value={hours} onChange={(e) => setHours(e.target.value)} required />
            </Field>
          )}
          <Field label="Ghi chú (không bắt buộc)"><textarea className={textarea} rows={3} maxLength={1000} value={note} onChange={(e) => setNote(e.target.value)} /></Field>
          <ErrorText>{error}</ErrorText>
        </AdminDialogBody>
        <AdminDialogFooter>
          <button type="button" className={btnGhost} onClick={onClose}>Hủy</button>
          <button type="submit" className={btnPrimary} disabled={saving}>{saving ? "Đang duyệt…" : "Duyệt"}</button>
        </AdminDialogFooter>
      </form>
    </AdminDialog>
  );
}
