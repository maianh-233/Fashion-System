import { useEffect, useState } from "react";
import PageGuide from "../../../components/admin/common/PageGuide";
import { Clock, LogIn, LogOut, MapPin, Plus } from "lucide-react";
import AdminCatalogPageHeader from "../../../components/admin/common/AdminCatalogPageHeader";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../../components/admin/common/AdminDialog";
import { myAttendanceApi } from "../../../api/admin/attendanceApi";
import {
  Badge, Empty, ErrorText, Field, Loading, Section, btnGhost, btnPrimary, card, control, textarea, useLoader,
} from "../../../components/admin/recruitment/RecruitmentUi";
import {
  EXCUSE_TYPES, OVERTIME_TYPES, RECORD_STATUS, REQUEST_STATUS, currentMonth, currentPosition, daysText, formatDate, formatTime,
  isoDate, minutesText, monthRange, shortTime,
} from "../attendance/attendanceLogic";

export default function MyAttendance() {
  const [version, setVersion] = useState(0);
  const refresh = () => setVersion((v) => v + 1);
  return (
    <div className="admin-catalog-page space-y-5 text-zinc-100 [&>*]:min-w-0">
      <AdminCatalogPageHeader icon={Clock} eyebrow="Cá nhân" title="Chấm công của tôi"
        description="Check-in / check-out, lịch sử công, đơn giải trình, nghỉ phép và tăng ca của bạn." />
      <PageGuide id="myAttendance" />
      <TodayCard version={version} onChanged={refresh} />
      <div className="grid gap-5 xl:grid-cols-[2fr_1fr] [&>*]:min-w-0">
        <History version={version} />
        <Balance version={version} />
      </div>
      <Requests version={version} onChanged={refresh} />
    </div>
  );
}

function Clock2() {
  const [now, setNow] = useState(new Date());
  useEffect(() => {
    const timer = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);
  return <span className="tabular-nums">{now.toLocaleTimeString("vi-VN")}</span>;
}

function TodayCard({ version, onChanged }) {
  const { data, error, loading, reload } = useLoader((signal) => myAttendanceApi.today({ signal }), [version]);
  const [busy, setBusy] = useState("");
  const [message, setMessage] = useState({ error: "", info: "" });
  const record = data?.record;

  const act = async (kind) => {
    setBusy(kind);
    setMessage({ error: "", info: "" });
    const { position, error: geoError } = await currentPosition();
    try {
      const body = position ? { latitude: position.latitude, longitude: position.longitude } : {};
      if (kind === "in") await myAttendanceApi.checkIn(body);
      else await myAttendanceApi.checkOut(body);
      setMessage({ error: "", info: `${kind === "in" ? "Check-in" : "Check-out"} thành công${geoError ? ` (không có vị trí: ${geoError})` : ""}.` });
      reload();
      onChanged();
    } catch (e) {
      setMessage({ error: geoError && data?.requireLocation ? `${e.message} (${geoError})` : e.message, info: "" });
    } finally {
      setBusy("");
    }
  };

  return (
    <section className={`${card} p-5`}>
      <ErrorText>{error}</ErrorText>
      {loading && !data ? <Loading /> : data && (
        <div className="grid gap-5 md:grid-cols-[1fr_auto] md:items-center">
          <div className="space-y-2">
            <p className="text-sm text-zinc-400">{formatDate(data.date)} · <Clock2 /></p>
            <p className="text-lg font-semibold">
              Ca hôm nay: {shortTime(data.expectedStart)} – {shortTime(data.expectedEnd)}
              <span className="ml-2 text-sm font-normal text-zinc-400">{data.employeeType === "STORE" ? `Cửa hàng ${data.storeName}` : "Nhân viên Global"}</span>
            </p>
            {(data.holiday || data.weeklyOff || data.leave) && (
              <p className="text-sm text-violet-300">{data.holiday ? `Ngày lễ: ${data.holiday}` : data.leave ? `Đang nghỉ phép: ${data.leave}` : "Hôm nay là ngày nghỉ tuần"}</p>
            )}
            <div className="flex flex-wrap gap-6 text-sm">
              <span>Vào: <b className="tabular-nums">{formatTime(record?.checkIn)}</b>{record?.lateMinutes > 0 && <span className="ml-1 text-amber-300">(trễ {record.lateMinutes}p)</span>}</span>
              <span>Ra: <b className="tabular-nums">{formatTime(record?.checkOut)}</b>{record?.earlyMinutes > 0 && <span className="ml-1 text-amber-300">(sớm {record.earlyMinutes}p)</span>}</span>
              {record?.workedMinutes != null && <span>Thời gian: <b>{minutesText(record.workedMinutes)}</b></span>}
            </div>
            {data.requireLocation && <p className="flex items-center gap-1 text-xs text-zinc-500"><MapPin size={13} />Cần bật định vị và ở tại cửa hàng khi chấm công.</p>}
          </div>
          <div className="flex gap-3">
            <button type="button" className={`${btnPrimary} px-6 py-3`} onClick={() => act("in")} disabled={Boolean(busy) || Boolean(record?.checkIn) || Boolean(record?.checkOut)}>
              <LogIn size={16} />{busy === "in" ? "Đang xử lý…" : "Check-in"}
            </button>
            <button type="button" className={`${btnGhost} px-6 py-3`} onClick={() => act("out")} disabled={Boolean(busy) || Boolean(record?.checkOut)}>
              <LogOut size={16} />{busy === "out" ? "Đang xử lý…" : "Check-out"}
            </button>
          </div>
        </div>
      )}
      <div className="mt-3 space-y-2">
        <ErrorText>{message.error}</ErrorText>
        {message.info && <p className="text-sm text-emerald-300">{message.info}</p>}
      </div>
    </section>
  );
}

function History({ version }) {
  const [month, setMonth] = useState(currentMonth());
  const { data, error, loading } = useLoader((signal) => myAttendanceApi.history(monthRange(month), { signal }), [month, version]);
  return (
    <Section title="Lịch sử chấm công" action={<input type="month" className={`${control} w-44`} value={month} max={currentMonth()} onChange={(e) => setMonth(e.target.value || currentMonth())} />}>
      <ErrorText>{error}</ErrorText>
      {loading && !data ? <Loading /> : (data || []).length === 0 ? <Empty>Chưa có dữ liệu chấm công trong tháng.</Empty> : (
        <div className="overflow-x-auto">
          <table className="min-w-full text-sm">
            <thead className="text-left text-xs uppercase text-zinc-400"><tr><th className="py-2 pr-3">Ngày</th><th className="py-2 pr-3">Vào</th><th className="py-2 pr-3">Ra</th><th className="py-2 pr-3">Thời gian</th><th className="py-2 pr-3">Trễ / sớm</th><th className="py-2">Trạng thái</th></tr></thead>
            <tbody className="divide-y divide-zinc-800">
              {data.map((r) => (
                <tr key={r.id}>
                  <td className="py-2 pr-3">{formatDate(r.date)}</td>
                  <td className="py-2 pr-3 tabular-nums">{formatTime(r.checkIn)}</td>
                  <td className="py-2 pr-3 tabular-nums">{formatTime(r.checkOut)}</td>
                  <td className="py-2 pr-3">{minutesText(r.workedMinutes)}</td>
                  <td className="py-2 pr-3 text-xs text-amber-300">{r.lateMinutes > 0 && `Trễ ${r.lateMinutes}p `}{r.earlyMinutes > 0 && `Sớm ${r.earlyMinutes}p`}</td>
                  <td className="py-2"><Badge map={RECORD_STATUS} value={r.status} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Section>
  );
}

function Balance({ version }) {
  const year = new Date().getFullYear();
  const { data, error, loading } = useLoader((signal) => myAttendanceApi.leaveBalance(year, { signal }), [version]);
  return (
    <Section title={`Ngày phép năm ${year}`}>
      <ErrorText>{error}</ErrorText>
      {loading && !data ? <Loading /> : (
        <ul className="space-y-2 text-sm">
          {(data || []).map((b) => (
            <li key={b.leaveTypeId} className="flex items-center justify-between rounded-xl border border-zinc-800 px-3 py-2">
              <span>{b.name}{!b.paid && <span className="ml-1 text-xs text-zinc-500">(không lương)</span>}</span>
              <span className="text-right tabular-nums text-zinc-300">
                {b.quota == null ? `Đã dùng ${daysText(b.used)}` : <>Còn <b className="text-emerald-300">{daysText(b.remaining)}</b> / {daysText(b.quota)}</>}
                {Number(b.pending) > 0 && <span className="block text-xs text-amber-300">Chờ duyệt {daysText(b.pending)}</span>}
              </span>
            </li>
          ))}
        </ul>
      )}
    </Section>
  );
}

const KINDS = [
  { value: "excuses", label: "Giải trình", list: myAttendanceApi.excuses, cancel: myAttendanceApi.cancelExcuse },
  { value: "leaves", label: "Nghỉ phép", list: myAttendanceApi.leaves, cancel: myAttendanceApi.cancelLeave },
  { value: "overtime", label: "Tăng ca", list: myAttendanceApi.overtime, cancel: myAttendanceApi.cancelOvertime },
];

function Requests({ version, onChanged }) {
  const [kind, setKind] = useState("excuses");
  const [creating, setCreating] = useState(false);
  const [actionError, setActionError] = useState("");
  const current = KINDS.find((k) => k.value === kind);
  const { data, error, loading, reload } = useLoader((signal) => current.list({ signal }), [kind, version]);
  const cancel = async (id) => {
    setActionError("");
    try {
      await current.cancel(id);
      reload();
      onChanged();
    } catch (e) {
      setActionError(e.message);
    }
  };
  return (
    <Section title="Đơn của tôi" action={<button type="button" className={btnPrimary} onClick={() => setCreating(true)}><Plus size={15} />Tạo đơn {current.label.toLowerCase()}</button>}>
      <div className="mb-4 flex gap-1">
        {KINDS.map((k) => (
          <button key={k.value} type="button" onClick={() => setKind(k.value)}
            className={`rounded-xl px-3 py-1.5 text-sm ${kind === k.value ? "bg-zinc-800 font-semibold text-amber-300" : "text-zinc-300 hover:text-white"}`}>{k.label}</button>
        ))}
      </div>
      <ErrorText>{error || actionError}</ErrorText>
      {loading && !data ? <Loading /> : (data || []).length === 0 ? <Empty>Bạn chưa có đơn nào.</Empty> : (
        <ul className="divide-y divide-zinc-800">
          {data.map((r) => (
            <li key={r.id} className="flex flex-wrap items-start justify-between gap-3 py-3 text-sm">
              <div>
                <div className="font-medium">{r.code} · <Summary kind={kind} r={r} /></div>
                <div className="text-zinc-400">{r.reason}</div>
                {r.rejectionReason && <div className="text-xs text-rose-300">Lý do từ chối: {r.rejectionReason}</div>}
                {r.approverNote && <div className="text-xs text-zinc-500">Ghi chú duyệt: {r.approverNote}</div>}
              </div>
              <div className="flex items-center gap-2">
                <Badge map={REQUEST_STATUS} value={r.status} />
                {r.status === "PENDING" && <button type="button" className="text-xs text-zinc-400 hover:text-rose-300" onClick={() => cancel(r.id)}>Hủy đơn</button>}
              </div>
            </li>
          ))}
        </ul>
      )}
      {creating && <CreateDialog kind={kind} onClose={() => setCreating(false)} onSaved={() => { setCreating(false); reload(); onChanged(); }} />}
    </Section>
  );
}

function Summary({ kind, r }) {
  if (kind === "excuses") return `${EXCUSE_TYPES.find((t) => t.value === r.type)?.label} ${formatDate(r.date)} (${r.proposedCheckIn ? `vào ${formatTime(r.proposedCheckIn)}` : ""}${r.proposedCheckOut ? ` ra ${formatTime(r.proposedCheckOut)}` : ""})`;
  if (kind === "leaves") return `${r.leaveTypeName} ${formatDate(r.startDate)}${r.endDate !== r.startDate ? ` – ${formatDate(r.endDate)}` : ""} (${daysText(r.totalDays)} ngày)`;
  return `${formatDate(r.workDate)} · ${OVERTIME_TYPES[r.type]} · ${r.hours} giờ${r.approvedHours != null ? ` (duyệt ${r.approvedHours})` : ""}`;
}

function CreateDialog({ kind, onClose, onSaved }) {
  const today = isoDate();
  const types = useLoader((signal) => (kind === "leaves" ? myAttendanceApi.leaveTypes({ signal }) : Promise.resolve([])), [kind]);
  const [form, setForm] = useState({
    date: today, type: "FORGOT_CHECK_OUT", checkIn: "", checkOut: "", leaveTypeId: "", startDate: today, endDate: today,
    halfDayStart: false, halfDayEnd: false, workDate: today, hours: "2", reason: "",
  });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const set = (patch) => setForm((f) => ({ ...f, ...patch }));

  const submit = async (event) => {
    event.preventDefault();
    if (!form.reason.trim()) return setError("Vui lòng nhập lý do");
    setSaving(true);
    setError("");
    try {
      if (kind === "excuses") {
        await myAttendanceApi.createExcuse({
          date: form.date, type: form.type, reason: form.reason.trim(),
          proposedCheckIn: form.checkIn ? `${form.date}T${form.checkIn}:00` : null,
          proposedCheckOut: form.checkOut ? `${form.date}T${form.checkOut}:00` : null,
        });
      } else if (kind === "leaves") {
        await myAttendanceApi.createLeave({
          leaveTypeId: form.leaveTypeId, startDate: form.startDate, endDate: form.endDate,
          halfDayStart: form.halfDayStart, halfDayEnd: form.halfDayEnd, reason: form.reason.trim(),
        });
      } else {
        await myAttendanceApi.createOvertime({ workDate: form.workDate, hours: Number(form.hours), reason: form.reason.trim() });
      }
      onSaved();
    } catch (e) {
      setError(e.message);
      setSaving(false);
    }
  };

  const sameDay = form.startDate === form.endDate;
  return (
    <AdminDialog onClose={onClose}>
      <form onSubmit={submit}>
        <AdminDialogHeader title={kind === "excuses" ? "Đơn giải trình chấm công" : kind === "leaves" ? "Đơn xin nghỉ phép" : "Đăng ký tăng ca"}
          description={kind === "excuses" ? "Dùng khi quên check-in / check-out. Công chỉ được cập nhật sau khi quản lý duyệt." : undefined} onClose={onClose} />
        <AdminDialogBody className="space-y-3">
          {kind === "excuses" && (
            <>
              <div className="grid gap-3 sm:grid-cols-2">
                <Field label="Ngày" required><input type="date" className={control} max={today} value={form.date} onChange={(e) => set({ date: e.target.value })} required /></Field>
                <Field label="Loại" required>
                  <select className={control} value={form.type} onChange={(e) => set({ type: e.target.value })}>
                    {EXCUSE_TYPES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
                  </select>
                </Field>
                {form.type === "FORGOT_CHECK_IN" && <Field label="Giờ vào thực tế" required><input type="time" className={control} value={form.checkIn} onChange={(e) => set({ checkIn: e.target.value })} required /></Field>}
                <Field label="Giờ ra thực tế" required={form.type === "FORGOT_CHECK_OUT"} hint={form.type === "FORGOT_CHECK_IN" ? "Điền nếu quên cả check-out" : undefined}>
                  <input type="time" className={control} value={form.checkOut} onChange={(e) => set({ checkOut: e.target.value })} required={form.type === "FORGOT_CHECK_OUT"} />
                </Field>
              </div>
            </>
          )}
          {kind === "leaves" && (
            <>
              <Field label="Loại nghỉ" required>
                <select className={control} value={form.leaveTypeId} onChange={(e) => set({ leaveTypeId: e.target.value })} required>
                  <option value="">— Chọn —</option>
                  {(types.data || []).map((t) => <option key={t.id} value={t.id}>{t.name}{t.paid ? "" : " (không lương)"}</option>)}
                </select>
              </Field>
              <div className="grid gap-3 sm:grid-cols-2">
                <Field label="Từ ngày" required><input type="date" className={control} value={form.startDate} onChange={(e) => set({ startDate: e.target.value, endDate: e.target.value > form.endDate ? e.target.value : form.endDate })} required /></Field>
                <Field label="Đến ngày" required><input type="date" className={control} min={form.startDate} value={form.endDate} onChange={(e) => set({ endDate: e.target.value })} required /></Field>
              </div>
              <div className="flex flex-wrap gap-4 text-sm text-zinc-300">
                <label className="flex items-center gap-2"><input type="checkbox" checked={form.halfDayStart} onChange={(e) => set({ halfDayStart: e.target.checked, halfDayEnd: sameDay ? false : form.halfDayEnd })} />{sameDay ? "Chỉ nghỉ nửa ngày" : "Ngày đầu nghỉ nửa ngày"}</label>
                {!sameDay && <label className="flex items-center gap-2"><input type="checkbox" checked={form.halfDayEnd} onChange={(e) => set({ halfDayEnd: e.target.checked })} />Ngày cuối nghỉ nửa ngày</label>}
              </div>
              <p className="text-xs text-zinc-500">Ngày nghỉ tuần và ngày lễ không bị trừ phép.</p>
            </>
          )}
          {kind === "overtime" && (
            <div className="grid gap-3 sm:grid-cols-2">
              <Field label="Ngày tăng ca" required><input type="date" className={control} value={form.workDate} onChange={(e) => set({ workDate: e.target.value })} required /></Field>
              <Field label="Số giờ" required hint="Bước 0.25 giờ, tối đa 12"><input type="number" min="0.5" max="12" step="0.25" className={control} value={form.hours} onChange={(e) => set({ hours: e.target.value })} required /></Field>
            </div>
          )}
          <Field label="Lý do" required><textarea className={textarea} rows={3} maxLength={1000} value={form.reason} onChange={(e) => set({ reason: e.target.value })} /></Field>
          <ErrorText>{error}</ErrorText>
        </AdminDialogBody>
        <AdminDialogFooter>
          <button type="button" className={btnGhost} onClick={onClose}>Hủy</button>
          <button type="submit" className={btnPrimary} disabled={saving}>{saving ? "Đang gửi…" : "Gửi đơn"}</button>
        </AdminDialogFooter>
      </form>
    </AdminDialog>
  );
}
