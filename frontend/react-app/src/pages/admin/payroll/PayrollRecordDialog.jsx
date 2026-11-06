import { useState } from "react";
import { Download, Trash2 } from "lucide-react";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../../components/admin/common/AdminDialog";
import { payrollApi } from "../../../api/admin/payrollApi";
import { saveBlob } from "../../../api/admin/attendanceApi";
import { Badge, ErrorText, Field, Loading, ReasonDialog, btnGhost, btnPrimary, control, useLoader } from "../../../components/admin/recruitment/RecruitmentUi";
import { formatDate, formatDateTime } from "../attendance/attendanceLogic";
import { usePayroll } from "./PayrollLayout";
import PayslipView from "./PayslipView";
import { ACTION_LABEL, KIND_LABEL, REVIEW_STATUS, money, num } from "./payrollLogic";

const DAY_TONE = { worked: "bg-emerald-500/70", paid: "bg-sky-500/70", unpaid: "bg-rose-500/70", holiday: "bg-violet-500/70", unverified: "bg-amber-500/70", off: "bg-zinc-800" };

export default function PayrollRecordDialog({ id, onClose, onChanged }) {
  const { can, components } = usePayroll();
  const { data, error, loading, setData } = useLoader((signal) => payrollApi.record(id, { signal }), [id]);
  const [removing, setRemoving] = useState(null);
  const [actionError, setActionError] = useState("");

  const apply = async (promise) => {
    setActionError("");
    try {
      setData(await promise);
      onChanged();
    } catch (e) {
      setActionError(e.message);
      throw e;
    }
  };

  return (
    <AdminDialog onClose={onClose} size="xl">
      <AdminDialogHeader title="Chi tiết lương" description={data ? `${data.row.fullName} · ${data.periodCode}` : ""} onClose={onClose} />
      <AdminDialogBody className="space-y-5">
        <ErrorText>{error || actionError}</ErrorText>
        {loading && !data && <Loading />}
        {data && (
          <>
            <div className="flex flex-wrap items-center gap-2">
              <Badge map={REVIEW_STATUS} value={data.row.reviewStatus} />
              {data.row.reviewNote && <span className="text-xs text-zinc-400">{data.row.reviewNote}</span>}
              <span className="ml-auto text-xs text-zinc-500">Đồng bộ công {formatDateTime(data.syncedAt)} · Tính lương {formatDateTime(data.calculatedAt) || "chưa"}</span>
            </div>
            {!data.row.calculated && <p className="rounded-xl border border-amber-500/30 bg-amber-500/10 px-3 py-2 text-sm text-amber-100">Dòng lương chưa được tính sau lần đồng bộ gần nhất — bấm “Tính lương” ở kỳ lương.</p>}
            <PayslipView detail={data} actions={data.editable ? (row) => row.source === "MANUAL" && (
              <button type="button" className="text-zinc-500 hover:text-rose-300" aria-label="Bỏ khoản" onClick={() => setRemoving(row)}><Trash2 size={14} /></button>
            ) : null} />
            <DayGrid days={data.days} />
            {data.editable && Number(data.row.unverifiedDays) > 0 && <ConfirmDaysForm detail={data} onSubmit={(body) => apply(payrollApi.confirmDays(id, body))} />}
            {data.editable && <AddItemForm components={components} onSubmit={(body) => apply(payrollApi.addItem(id, body))} />}
            {data.closed && can.manage && <AfterCloseForm onSubmit={(body) => apply(payrollApi.addAdjustment(id, body))} />}
            <Adjustments detail={data} canCancel={can.manage} onCancel={(adjId) => apply(payrollApi.cancelAdjustment(id, adjId)).catch(() => {})} />
          </>
        )}
      </AdminDialogBody>
      <AdminDialogFooter>
        {data && can.export && <button type="button" className={btnGhost} onClick={async () => {
          try { saveBlob(await payrollApi.recordPdf(id), `phieu-luong_${data.row.employeeCode || id}.pdf`); } catch (e) { setActionError(e.message); }
        }}><Download size={15} />Phiếu lương PDF</button>}
        <button type="button" className={btnGhost} onClick={onClose}>Đóng</button>
      </AdminDialogFooter>
      {removing && <ReasonDialog title={`Bỏ khoản “${removing.name}”`} label="Lý do" confirmText="Bỏ khoản" danger onClose={() => setRemoving(null)}
        onConfirm={async (reason) => { await apply(payrollApi.removeItem(id, removing.id, reason)); setRemoving(null); }} />}
    </AdminDialog>
  );
}

function DayGrid({ days }) {
  if (!Array.isArray(days) || days.length === 0) return null;
  const kind = (d) => {
    if (!d.workingDay) return Number(d.worked) > 0 || d.offDayWorked ? "worked" : "off";
    if (d.holiday) return "holiday";
    if (Number(d.unpaidLeave) > 0) return "unpaid";
    if (Number(d.paidLeave) > 0) return "paid";
    if (Number(d.unverified) > 0) return "unverified";
    return "worked";
  };
  const label = { worked: "Đi làm", paid: "Nghỉ có lương", unpaid: "Nghỉ không lương", holiday: "Ngày lễ", unverified: "Chưa xác minh", off: "Nghỉ tuần" };
  return (
    <div>
      <h3 className="mb-2 text-sm font-semibold text-zinc-200">Dữ liệu công làm căn cứ tính lương</h3>
      <div className="flex flex-wrap gap-1">
        {days.map((d) => {
          const k = kind(d);
          return (
            <span key={d.date} title={`${formatDate(d.date)}: ${label[k]}${d.leaveType ? ` (${d.leaveType})` : ""}${d.holiday ? ` (${d.holiday})` : ""}${d.recordStatus ? ` · ${d.recordStatus}` : ""}`}
              className={`flex h-8 w-8 items-center justify-center rounded-lg text-xs tabular-nums text-white ${DAY_TONE[k]}`}>
              {String(d.date).slice(8, 10)}
            </span>
          );
        })}
      </div>
      <div className="mt-2 flex flex-wrap gap-3 text-xs text-zinc-400">
        {Object.entries(label).map(([k, l]) => <span key={k} className="flex items-center gap-1"><span className={`h-3 w-3 rounded ${DAY_TONE[k]}`} />{l}</span>)}
      </div>
    </div>
  );
}

function ConfirmDaysForm({ detail, onSubmit }) {
  const [days, setDays] = useState(String(detail.row.confirmedAbsentDays ?? 0));
  const [note, setNote] = useState(detail.row.reviewStatus === "CONFIRMED" ? detail.row.reviewNote || "" : "");
  const [saving, setSaving] = useState(false);
  return (
    <form className="space-y-3 rounded-2xl border border-amber-500/30 p-4" onSubmit={async (e) => {
      e.preventDefault();
      setSaving(true);
      try { await onSubmit({ confirmedAbsentDays: Number(days), note: note.trim() }); } catch { /* lỗi hiển thị ở trên */ } finally { setSaving(false); }
    }}>
      <h3 className="text-sm font-semibold text-amber-200">Xác minh {num(detail.row.unverifiedDays)} ngày chưa có dữ liệu chấm công</h3>
      <p className="text-xs text-zinc-400">Hệ thống không tự trừ lương các ngày này. Nhập số ngày thực sự vắng không lương (0 nếu nhân viên có đi làm / nghỉ hợp lệ).</p>
      <div className="grid gap-3 sm:grid-cols-[160px_1fr]">
        <Field label="Số ngày vắng không lương"><input type="number" min="0" max={detail.row.unverifiedDays} step="0.5" className={control} value={days} onChange={(e) => setDays(e.target.value)} required /></Field>
        <Field label="Căn cứ xác minh" required><input className={control} maxLength={1000} value={note} onChange={(e) => setNote(e.target.value)} required /></Field>
      </div>
      <div className="flex justify-end"><button type="submit" className={btnPrimary} disabled={saving}>{saving ? "Đang lưu…" : "Xác nhận ngày công"}</button></div>
    </form>
  );
}

function AddItemForm({ components, onSubmit }) {
  const [form, setForm] = useState({ componentId: "", name: "", kind: "BONUS", amount: "", note: "", reason: "" });
  const [saving, setSaving] = useState(false);
  const manual = components.filter((c) => c.active);
  const pick = (componentId) => {
    const c = manual.find((x) => x.id === componentId);
    setForm((f) => ({ ...f, componentId, name: c ? c.name : f.name, kind: c ? c.kind : f.kind, amount: c?.defaultAmount ?? f.amount }));
  };
  return (
    <form className="space-y-3 rounded-2xl border border-zinc-800 p-4" onSubmit={async (e) => {
      e.preventDefault();
      setSaving(true);
      try {
        await onSubmit({ ...form, componentId: form.componentId || null, amount: Number(form.amount), note: form.note.trim() || null, name: form.name.trim(), reason: form.reason.trim() });
        setForm({ componentId: "", name: "", kind: "BONUS", amount: "", note: "", reason: "" });
      } catch { /* lỗi hiển thị ở trên */ } finally { setSaving(false); }
    }}>
      <h3 className="text-sm font-semibold text-zinc-200">Thêm khoản trong kỳ (thưởng, phụ cấp, khấu trừ khác)</h3>
      <div className="grid gap-3 md:grid-cols-4">
        <Field label="Theo danh mục">
          <select className={control} value={form.componentId} onChange={(e) => pick(e.target.value)}>
            <option value="">— Khoản khác —</option>
            {manual.map((c) => <option key={c.id} value={c.id}>{KIND_LABEL[c.kind]}: {c.name}</option>)}
          </select>
        </Field>
        <Field label="Tên khoản" required><input className={control} maxLength={150} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required /></Field>
        <Field label="Loại">
          <select className={control} value={form.kind} disabled={Boolean(form.componentId)} onChange={(e) => setForm({ ...form, kind: e.target.value })}>
            <option value="ALLOWANCE">Phụ cấp</option><option value="BONUS">Thưởng</option><option value="DEDUCTION">Khấu trừ</option>
          </select>
        </Field>
        <Field label="Số tiền (₫)" required><input type="number" min="0" step="1000" className={control} value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} required /></Field>
        <Field label="Ghi chú trên phiếu lương" className="md:col-span-2"><input className={control} maxLength={500} value={form.note} onChange={(e) => setForm({ ...form, note: e.target.value })} /></Field>
        <Field label="Lý do (lưu lịch sử)" required className="md:col-span-2"><input className={control} maxLength={1000} value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} required /></Field>
      </div>
      <div className="flex justify-end"><button type="submit" className={btnPrimary} disabled={saving}>{saving ? "Đang lưu…" : "Thêm khoản"}</button></div>
    </form>
  );
}

function AfterCloseForm({ onSubmit }) {
  const [form, setForm] = useState({ kind: "BONUS", name: "", amount: "", reason: "" });
  const [saving, setSaving] = useState(false);
  return (
    <form className="space-y-3 rounded-2xl border border-violet-500/30 p-4" onSubmit={async (e) => {
      e.preventDefault();
      setSaving(true);
      try {
        await onSubmit({ ...form, name: form.name.trim(), reason: form.reason.trim(), amount: Number(form.amount) });
        setForm({ kind: "BONUS", name: "", amount: "", reason: "" });
      } catch { /* lỗi hiển thị ở trên */ } finally { setSaving(false); }
    }}>
      <h3 className="text-sm font-semibold text-violet-200">Điều chỉnh sau chốt</h3>
      <p className="text-xs text-zinc-400">Bảng lương đã chốt giữ nguyên. Khoản điều chỉnh (truy lĩnh / truy thu) được đưa vào kỳ lương kế tiếp của nhân viên và lưu lịch sử.</p>
      <div className="grid gap-3 md:grid-cols-4">
        <Field label="Loại">
          <select className={control} value={form.kind} onChange={(e) => setForm({ ...form, kind: e.target.value })}>
            <option value="BONUS">Truy lĩnh / thưởng</option><option value="ALLOWANCE">Phụ cấp bổ sung</option><option value="DEDUCTION">Truy thu / khấu trừ</option>
          </select>
        </Field>
        <Field label="Tên khoản" required><input className={control} maxLength={150} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required /></Field>
        <Field label="Số tiền (₫)" required><input type="number" min="1" step="1000" className={control} value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} required /></Field>
        <Field label="Lý do" required><input className={control} maxLength={1000} value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} required /></Field>
      </div>
      <div className="flex justify-end"><button type="submit" className={btnPrimary} disabled={saving}>{saving ? "Đang lưu…" : "Tạo điều chỉnh"}</button></div>
    </form>
  );
}

function Adjustments({ detail, canCancel, onCancel }) {
  if (!detail.adjustments.length) return null;
  return (
    <div>
      <h3 className="mb-2 text-sm font-semibold text-zinc-200">Lịch sử điều chỉnh</h3>
      <ul className="space-y-2 text-sm">
        {detail.adjustments.map((a) => (
          <li key={a.id} className="rounded-xl border border-zinc-800 px-3 py-2">
            <div className="flex flex-wrap justify-between gap-2">
              <span><b>{ACTION_LABEL[a.action] || a.action}</b>{a.name && ` · ${a.name}`}{a.amount != null && ` · ${a.kind === "DEDUCTION" ? "−" : "+"}${money(a.amount)}`}</span>
              <span className="text-xs text-zinc-500">{a.createdByName} · {formatDateTime(a.createdAt)}</span>
            </div>
            <p className="text-zinc-400">{a.reason}</p>
            {a.phase === "AFTER_CLOSE" && (
              <p className="text-xs text-zinc-500">
                {a.status === "PENDING" ? (a.appliedPeriodCode ? `Đưa vào kỳ ${a.appliedPeriodCode} (chờ chốt)` : "Chờ đưa vào kỳ lương kế tiếp") : a.status === "APPLIED" ? `Đã áp dụng ở kỳ ${a.appliedPeriodCode}` : "Đã hủy"}
                {canCancel && a.status === "PENDING" && <button type="button" className="ml-2 text-rose-300 hover:underline" onClick={() => onCancel(a.id)}>Hủy</button>}
              </p>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}
