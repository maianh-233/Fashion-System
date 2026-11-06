import { useState } from "react";
import Pagination from "../../../components/common/Pagination";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, Banknote, Calculator, CheckCircle2, Download, RefreshCw, Search, Send, XCircle } from "lucide-react";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../../components/admin/common/AdminDialog";
import { payrollApi } from "../../../api/admin/payrollApi";
import { saveBlob } from "../../../api/admin/attendanceApi";
import {
  Badge, Empty, ErrorText, Field, Loading, ReasonDialog, Section, Stat, btnDanger, btnGhost, btnPrimary, control, textarea, useLoader,
} from "../../../components/admin/recruitment/RecruitmentUi";
import { formatDateTime, isoDate } from "../attendance/attendanceLogic";
import { usePayroll } from "./PayrollLayout";
import PayrollRecordDialog from "./PayrollRecordDialog";
import { ACTION_LABEL, PAYMENT_STATUS, PERIOD_STATUS, REVIEW_STATUS, money, monthLabel, num } from "./payrollLogic";

export default function PayrollPeriodDetail() {
  const { id } = useParams();
  const { can } = usePayroll();
  const detail = useLoader((signal) => payrollApi.period(id, { signal }), [id]);
  const [filter, setFilter] = useState({ keyword: "", reviewStatus: "" });
  const [keyword, setKeyword] = useState("");
  const [page, setPage] = useState(1);
  const rows = useLoader((signal) => payrollApi.periodRecords(id, { ...filter, page: page - 1, size: 20 }, { signal }), [id, JSON.stringify(filter), page, detail.data?.period?.status, detail.data?.period?.calculatedAt]);
  const [busy, setBusy] = useState("");
  const [message, setMessage] = useState({ error: "", info: "" });
  const [syncResult, setSyncResult] = useState(null);
  const [dialog, setDialog] = useState(null);
  const [selected, setSelected] = useState(null);

  const refresh = () => { detail.reload(); rows.reload(); };
  const run = async (key, action, info) => {
    setBusy(key);
    setMessage({ error: "", info: "" });
    try {
      const result = await action();
      if (key === "sync") setSyncResult(result);
      setMessage({ error: "", info: typeof info === "function" ? info(result) : info });
      refresh();
    } catch (e) {
      setMessage({ error: e.message, info: "" });
    } finally {
      setBusy("");
    }
  };

  if (detail.loading && !detail.data) return <Loading />;
  if (detail.error) return <ErrorText>{detail.error}</ErrorText>;
  const { period, approvals, actions, blockers } = detail.data;

  return (
    <div className="space-y-5">
      <Link to="/admin/payroll/periods" className="inline-flex items-center gap-1 text-sm text-zinc-400 hover:text-white"><ArrowLeft size={14} />Danh sách kỳ lương</Link>
      <Section>
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h2 className="text-xl font-semibold">Kỳ lương tháng {monthLabel(period.periodMonth)} · {period.storeName || "Văn phòng (Global)"}</h2>
            <p className="mt-1 text-sm text-zinc-400">{period.code} · Ngày công chuẩn: {period.standardDays ? num(period.standardDays) : "—"} · Đồng bộ công: {formatDateTime(period.attendanceSyncedAt) || "chưa"}</p>
            {period.lastRejectionReason && period.status === "CALCULATED" && <p className="mt-2 text-sm text-rose-300">Bị từ chối: {period.lastRejectionReason}</p>}
            {period.cancelReason && <p className="mt-2 text-sm text-rose-300">Lý do hủy: {period.cancelReason}</p>}
          </div>
          <Badge map={PERIOD_STATUS} value={period.status} />
        </div>
        <div className="mt-4 flex flex-wrap gap-2">
          {actions.sync && <button type="button" className={btnGhost} disabled={Boolean(busy)} onClick={() => run("sync", () => payrollApi.sync(id), (r) => `Đã đồng bộ: thêm ${r.added}, cập nhật ${r.updated}, loại ${r.removed}. Hãy tính lại lương.`)}><RefreshCw size={15} />{busy === "sync" ? "Đang đồng bộ…" : "Đồng bộ dữ liệu công"}</button>}
          {actions.calculate && <button type="button" className={btnPrimary} disabled={Boolean(busy)} onClick={() => run("calc", () => payrollApi.calculate(id), "Đã tính lương. Kiểm tra từng nhân viên trước khi gửi duyệt.")}><Calculator size={15} />{busy === "calc" ? "Đang tính…" : period.status === "CALCULATED" ? "Tính lại" : "Tính lương"}</button>}
          {can.manage && period.status === "CALCULATED" && <button type="button" className={btnPrimary} disabled={Boolean(busy) || !actions.submit} title={actions.submit ? undefined : blockers.join("; ")} onClick={() => setDialog("submit")}><Send size={15} />Gửi duyệt</button>}
          {actions.approve && <button type="button" className={btnPrimary} disabled={Boolean(busy)} onClick={() => setDialog("approve")}><CheckCircle2 size={15} />Phê duyệt & chốt</button>}
          {actions.approve && <button type="button" className={btnDanger} disabled={Boolean(busy)} onClick={() => setDialog("reject")}><XCircle size={15} />Từ chối</button>}
          {actions.pay && <button type="button" className={btnPrimary} disabled={Boolean(busy)} onClick={() => setDialog("pay")}><Banknote size={15} />Xác nhận thanh toán</button>}
          {can.export && period.employeeCount > 0 && <button type="button" className={btnGhost} disabled={Boolean(busy)} onClick={() => run("export", async () => saveBlob(await payrollApi.exportPeriod(id), `${period.code}.xlsx`), "Đã xuất Excel.")}><Download size={15} />Xuất Excel</button>}
          {actions.cancel && <button type="button" className={btnDanger} disabled={Boolean(busy)} onClick={() => setDialog("cancel")}>Hủy kỳ lương</button>}
        </div>
        <div className="mt-3 space-y-2">
          <ErrorText>{message.error}</ErrorText>
          {message.info && <p className="text-sm text-emerald-300">{message.info}</p>}
          {period.status === "CALCULATED" && blockers.length > 0 && (
            <ul className="rounded-xl border border-amber-500/30 bg-amber-500/10 px-4 py-2 text-sm text-amber-100">
              {blockers.map((b) => <li key={b}>• {b}</li>)}
            </ul>
          )}
          {syncResult?.skipped?.length > 0 && (
            <details className="rounded-xl border border-zinc-700 px-4 py-2 text-sm text-zinc-300">
              <summary className="cursor-pointer">{syncResult.skipped.length} nhân viên không được đưa vào kỳ lương</summary>
              <ul className="mt-2 space-y-1">{syncResult.skipped.map((s) => <li key={s.userId}>{s.employeeCode} · {s.fullName}: <span className="text-amber-300">{s.reason}</span></li>)}</ul>
            </details>
          )}
        </div>
      </Section>

      <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
        <Stat label="Nhân viên" value={period.employeeCount} hint={period.needsReviewCount ? `${period.needsReviewCount} cần xem xét` : undefined} tone={period.needsReviewCount ? "amber" : "zinc"} />
        <Stat label="Tổng thu nhập" value={money(period.totalGross)} tone="amber" />
        <Stat label="Tổng khấu trừ" value={money(period.totalDeduction)} tone="rose" />
        <Stat label="Tổng thực nhận" value={money(period.totalNet)} tone="emerald" hint={`${period.paidCount}/${period.employeeCount} đã thanh toán`} />
      </div>

      <Section title="Bảng lương">
        <form className="mb-4 grid gap-3 md:grid-cols-4" onSubmit={(e) => { e.preventDefault(); setPage(1); setFilter((f) => ({ ...f, keyword: keyword.trim() })); }}>
          <Field label="Tìm nhân viên" className="md:col-span-2">
            <div className="flex gap-2"><input className={control} value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="Mã, tên, phòng ban" /><button type="submit" className={btnGhost} aria-label="Tìm"><Search size={15} /></button></div>
          </Field>
          <Field label="Dữ liệu công">
            <select className={control} value={filter.reviewStatus} onChange={(e) => { setPage(1); setFilter((f) => ({ ...f, reviewStatus: e.target.value })); }}>
              <option value="">Tất cả</option>
              {Object.entries(REVIEW_STATUS).map(([v, s]) => <option key={v} value={v}>{s.label}</option>)}
            </select>
          </Field>
        </form>
        <ErrorText>{rows.error}</ErrorText>
        <div className="overflow-x-auto">
          <table className="min-w-full whitespace-nowrap text-sm">
            <thead className="border-b border-zinc-800 text-left text-xs uppercase tracking-wide text-zinc-400">
              <tr>
                <th className="px-3 py-2">Nhân viên</th><th className="px-3 py-2">Phòng ban / chức vụ</th><th className="px-3 py-2 text-right">Lương CB</th>
                <th className="px-3 py-2 text-right">Công chuẩn</th><th className="px-3 py-2 text-right">Công tính lương</th><th className="px-3 py-2 text-right">Nghỉ có lương</th>
                <th className="px-3 py-2 text-right">Nghỉ không lương</th><th className="px-3 py-2 text-right">Phụ cấp</th><th className="px-3 py-2 text-right">Thưởng</th>
                <th className="px-3 py-2 text-right">Tăng ca</th><th className="px-3 py-2 text-right">Khấu trừ</th><th className="px-3 py-2 text-right">Thực nhận</th><th className="px-3 py-2">Trạng thái</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-800">
              {rows.loading && !rows.data && <tr><td colSpan="13"><Loading /></td></tr>}
              {rows.data?.content?.length === 0 && <tr><td colSpan="13"><Empty>{period.attendanceSyncedAt ? "Không có nhân viên phù hợp." : "Bấm “Đồng bộ dữ liệu công” để lập danh sách nhân viên."}</Empty></td></tr>}
              {rows.data?.content?.map((r) => (
                <tr key={r.id} className="cursor-pointer hover:bg-zinc-800/40" onClick={() => setSelected(r.id)}>
                  <td className="px-3 py-2"><div className="font-medium">{r.fullName}</div><div className="text-xs text-zinc-500">{r.employeeCode}</div></td>
                  <td className="px-3 py-2 text-xs text-zinc-400">{r.departmentName || "—"}<br />{r.positionName || ""}</td>
                  <td className="px-3 py-2 text-right tabular-nums">{money(r.baseSalary)}</td>
                  <td className="px-3 py-2 text-right tabular-nums">{num(r.standardDays)}</td>
                  <td className="px-3 py-2 text-right tabular-nums">{num(r.payableDays)}</td>
                  <td className="px-3 py-2 text-right tabular-nums">{num(r.paidLeaveDays)}</td>
                  <td className="px-3 py-2 text-right tabular-nums">{num(Number(r.unpaidLeaveDays) + Number(r.confirmedAbsentDays))}</td>
                  <td className="px-3 py-2 text-right tabular-nums">{money(r.allowanceTotal)}</td>
                  <td className="px-3 py-2 text-right tabular-nums">{money(r.bonusTotal)}</td>
                  <td className="px-3 py-2 text-right tabular-nums">{money(r.overtimePay)}</td>
                  <td className="px-3 py-2 text-right tabular-nums text-rose-300">{money(r.deductionTotal)}</td>
                  <td className="px-3 py-2 text-right font-semibold tabular-nums text-emerald-300">{r.calculated ? money(r.netPay) : <span className="text-xs font-normal text-zinc-500">chưa tính</span>}</td>
                  <td className="px-3 py-2"><div className="flex gap-1"><Badge map={REVIEW_STATUS} value={r.reviewStatus} />{period.status !== "DRAFT" && period.status !== "CALCULATED" && <Badge map={PAYMENT_STATUS} value={r.paymentStatus} />}</div></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div className="mt-4"><Pagination currentPage={page} totalPages={rows.data?.page?.totalPages || rows.data?.totalPages || 1} onPageChange={setPage} /></div>
      </Section>

      <Section title="Lịch sử phê duyệt">
        <ul className="space-y-2 text-sm">
          {approvals.map((a, i) => (
            <li key={i} className="flex flex-wrap justify-between gap-2 rounded-xl border border-zinc-800 px-3 py-2">
              <span><b>{ACTION_LABEL[a.action] || a.action}</b>{a.note && <span className="text-zinc-400"> — {a.note}</span>}</span>
              <span className="text-xs text-zinc-500">{a.actorName || "Hệ thống"} · {formatDateTime(a.createdAt)}</span>
            </li>
          ))}
        </ul>
      </Section>

      {selected && <PayrollRecordDialog id={selected} onClose={() => setSelected(null)} onChanged={refresh} />}
      {dialog === "submit" && <NoteDialog title="Gửi duyệt bảng lương" description="Sau khi gửi, dữ liệu công tháng này của các nhân viên trong bảng lương bị khóa." confirmText="Gửi duyệt"
        onClose={() => setDialog(null)} onConfirm={(note) => run("submit", () => payrollApi.submit(id, note), "Đã gửi phê duyệt.").then(() => setDialog(null))} />}
      {dialog === "approve" && <NoteDialog title="Phê duyệt & chốt lương" description="Bảng lương đã chốt không sửa trực tiếp được; phiếu lương được phát hành cho nhân viên." confirmText="Phê duyệt"
        onClose={() => setDialog(null)} onConfirm={(note) => run("approve", () => payrollApi.approve(id, note), "Đã chốt lương và phát hành phiếu lương.").then(() => setDialog(null))} />}
      {dialog === "reject" && <ReasonDialog title="Từ chối bảng lương" label="Lý do từ chối" danger confirmText="Từ chối" onClose={() => setDialog(null)}
        onConfirm={async (reason) => { await payrollApi.reject(id, reason); setDialog(null); refresh(); }} />}
      {dialog === "cancel" && <ReasonDialog title="Hủy kỳ lương" description="Nhân viên trong kỳ được giải phóng để lập kỳ lương khác." label="Lý do hủy" danger confirmText="Hủy kỳ lương" onClose={() => setDialog(null)}
        onConfirm={async (reason) => { await payrollApi.cancel(id, reason); setDialog(null); refresh(); }} />}
      {dialog === "pay" && <PayDialog onClose={() => setDialog(null)} onConfirm={async (body) => {
        const result = await payrollApi.pay(id, body);
        setDialog(null);
        setMessage({ error: "", info: `Đã xác nhận ${result.paid} phiếu lương${result.alreadyPaid ? `, ${result.alreadyPaid} phiếu đã trả trước đó` : ""}.` });
        refresh();
      }} />}
    </div>
  );
}

function NoteDialog({ title, description, confirmText, onClose, onConfirm }) {
  const [note, setNote] = useState("");
  const [saving, setSaving] = useState(false);
  return (
    <AdminDialog onClose={onClose}>
      <form onSubmit={async (e) => { e.preventDefault(); setSaving(true); await onConfirm(note.trim() || null); setSaving(false); }}>
        <AdminDialogHeader title={title} description={description} onClose={onClose} />
        <AdminDialogBody><Field label="Ghi chú (không bắt buộc)"><textarea className={textarea} rows={3} maxLength={1000} value={note} onChange={(e) => setNote(e.target.value)} /></Field></AdminDialogBody>
        <AdminDialogFooter>
          <button type="button" className={btnGhost} onClick={onClose}>Hủy</button>
          <button type="submit" className={btnPrimary} disabled={saving}>{saving ? "Đang xử lý…" : confirmText}</button>
        </AdminDialogFooter>
      </form>
    </AdminDialog>
  );
}

function PayDialog({ onClose, onConfirm }) {
  const [form, setForm] = useState({ paidDate: isoDate(), reference: "", note: "" });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      await onConfirm({ paidDate: form.paidDate, reference: form.reference.trim() || null, note: form.note.trim() || null });
    } catch (e) {
      setError(e.message);
      setSaving(false);
    }
  };
  return (
    <AdminDialog onClose={onClose}>
      <form onSubmit={submit}>
        <AdminDialogHeader title="Xác nhận đã thanh toán" description="Ghi nhận thủ công cho mọi phiếu lương chưa trả trong kỳ. Mỗi phiếu chỉ được thanh toán một lần." onClose={onClose} />
        <AdminDialogBody className="space-y-3">
          <Field label="Ngày thanh toán" required><input type="date" className={control} max={isoDate()} value={form.paidDate} onChange={(e) => setForm({ ...form, paidDate: e.target.value })} required /></Field>
          <Field label="Số chứng từ / mã giao dịch"><input className={control} maxLength={100} value={form.reference} onChange={(e) => setForm({ ...form, reference: e.target.value })} /></Field>
          <Field label="Ghi chú"><textarea className={textarea} rows={2} maxLength={500} value={form.note} onChange={(e) => setForm({ ...form, note: e.target.value })} /></Field>
          <ErrorText>{error}</ErrorText>
        </AdminDialogBody>
        <AdminDialogFooter>
          <button type="button" className={btnGhost} onClick={onClose}>Hủy</button>
          <button type="submit" className={btnPrimary} disabled={saving}>{saving ? "Đang lưu…" : "Xác nhận"}</button>
        </AdminDialogFooter>
      </form>
    </AdminDialog>
  );
}
