import { useState } from "react";
import { Plus, Trash2 } from "lucide-react";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../../components/admin/common/AdminDialog";
import { attendanceApi } from "../../../api/admin/attendanceApi";
import {
  Badge, Empty, ErrorText, Field, Loading, Section, btnGhost, btnPrimary, control, textarea, useLoader,
} from "../../../components/admin/recruitment/RecruitmentUi";
import { useAttendance } from "./AttendanceLayout";
import { WEEKDAYS, daysText, formatDate, shortTime } from "./attendanceLogic";

export default function AttendanceSettings() {
  const { can } = useAttendance();
  const editable = Boolean(can.globalAdmin);
  return (
    <div className="space-y-5">
      {!editable && <p className="text-sm text-zinc-400">Chỉ quản trị chấm công cấp chuỗi được thay đổi cấu hình. Bạn đang xem ở chế độ chỉ đọc.</p>}
      <GeneralSettings editable={editable} />
      <Holidays editable={editable} />
      <LeaveTypes editable={editable} />
    </div>
  );
}

const toForm = (data) => ({ ...data, defaultCheckIn: shortTime(data.defaultCheckIn), defaultCheckOut: shortTime(data.defaultCheckOut) });

function GeneralSettings({ editable }) {
  const { data, error, loading } = useLoader((signal) => attendanceApi.settings({ signal }), []);
  return (
    <Section title="Giờ làm & quy tắc chung">
      <ErrorText>{error}</ErrorText>
      {loading && !data ? <Loading /> : data && <SettingsForm initial={data} editable={editable} />}
    </Section>
  );
}

function SettingsForm({ initial, editable }) {
  const [form, setForm] = useState(() => toForm(initial));
  const [message, setMessage] = useState({ error: "", ok: "" });
  const [saving, setSaving] = useState(false);

  const toggleDay = (day) => setForm((f) => ({
    ...f, weeklyOffDays: f.weeklyOffDays.includes(day) ? f.weeklyOffDays.filter((d) => d !== day) : [...f.weeklyOffDays, day],
  }));
  const save = async (event) => {
    event.preventDefault();
    setSaving(true);
    setMessage({ error: "", ok: "" });
    try {
      const saved = await attendanceApi.updateSettings({
        ...form, standardHoursPerDay: Number(form.standardHoursPerDay), storeRadiusMeters: Number(form.storeRadiusMeters),
        lateThresholdMinutes: Number(form.lateThresholdMinutes), earlyThresholdMinutes: Number(form.earlyThresholdMinutes),
        defaultCheckIn: `${form.defaultCheckIn}:00`, defaultCheckOut: `${form.defaultCheckOut}:00`,
      });
      setForm(toForm(saved));
      setMessage({ error: "", ok: "Đã lưu cấu hình." });
    } catch (e) {
      setMessage({ error: e.message, ok: "" });
    } finally {
      setSaving(false);
    }
  };

  return (
        <form className="space-y-4" onSubmit={save}>
          <fieldset disabled={!editable} className="space-y-4">
            <div className="grid gap-3 md:grid-cols-4">
              <Field label="Giờ vào mặc định"><input type="time" className={control} value={form.defaultCheckIn} onChange={(e) => setForm({ ...form, defaultCheckIn: e.target.value })} required /></Field>
              <Field label="Giờ ra mặc định"><input type="time" className={control} value={form.defaultCheckOut} onChange={(e) => setForm({ ...form, defaultCheckOut: e.target.value })} required /></Field>
              <Field label="Cho phép trễ (phút)"><input type="number" min="0" max="240" className={control} value={form.lateThresholdMinutes} onChange={(e) => setForm({ ...form, lateThresholdMinutes: e.target.value })} /></Field>
              <Field label="Cho phép về sớm (phút)"><input type="number" min="0" max="240" className={control} value={form.earlyThresholdMinutes} onChange={(e) => setForm({ ...form, earlyThresholdMinutes: e.target.value })} /></Field>
              <Field label="Giờ công chuẩn / ngày" hint="Dùng tính đơn giá tăng ca"><input type="number" min="1" max="24" step="0.5" className={control} value={form.standardHoursPerDay} onChange={(e) => setForm({ ...form, standardHoursPerDay: e.target.value })} /></Field>
              <Field label="Bán kính cửa hàng (m)"><input type="number" min="20" max="5000" className={control} value={form.storeRadiusMeters} onChange={(e) => setForm({ ...form, storeRadiusMeters: e.target.value })} /></Field>
              <label className="flex items-center gap-2 text-sm text-zinc-300 md:col-span-2">
                <input type="checkbox" checked={form.requireStoreLocation} onChange={(e) => setForm({ ...form, requireStoreLocation: e.target.checked })} />
                Bắt buộc nhân viên cửa hàng bật định vị và ở trong bán kính khi chấm công
              </label>
            </div>
            <div>
              <p className="mb-2 text-xs font-medium text-zinc-400">Ngày nghỉ hằng tuần</p>
              <div className="flex flex-wrap gap-2">
                {WEEKDAYS.map((d) => (
                  <button key={d.value} type="button" onClick={() => toggleDay(d.value)}
                    className={`rounded-xl border px-3 py-1.5 text-sm ${form.weeklyOffDays.includes(d.value) ? "border-amber-400 bg-amber-400/10 text-amber-300" : "border-zinc-700 text-zinc-300"}`}>
                    {d.label}
                  </button>
                ))}
              </div>
            </div>
          </fieldset>
          <ErrorText>{message.error}</ErrorText>
          {message.ok && <p className="text-sm text-emerald-300">{message.ok}</p>}
          {editable && <div className="flex justify-end"><button type="submit" className={btnPrimary} disabled={saving}>{saving ? "Đang lưu…" : "Lưu cấu hình"}</button></div>}
        </form>
  );
}

function Holidays({ editable }) {
  const [year, setYear] = useState(new Date().getFullYear());
  const { data, error, loading, setData } = useLoader((signal) => attendanceApi.holidays(year, { signal }), [year]);
  const [form, setForm] = useState({ date: "", name: "" });
  const [actionError, setActionError] = useState("");
  const add = async (event) => {
    event.preventDefault();
    setActionError("");
    try {
      const list = await attendanceApi.addHoliday({ date: form.date, name: form.name.trim() });
      if (Number(form.date.slice(0, 4)) === year) setData(list);
      setForm({ date: "", name: "" });
    } catch (e) {
      setActionError(e.message);
    }
  };
  const remove = async (date) => {
    setActionError("");
    try {
      setData(await attendanceApi.deleteHoliday(date));
    } catch (e) {
      setActionError(e.message);
    }
  };
  return (
    <Section title="Ngày lễ (nghỉ có lương)" action={
      <select className={`${control} w-28`} value={year} onChange={(e) => setYear(Number(e.target.value))}>
        {[-1, 0, 1].map((d) => new Date().getFullYear() + d).map((y) => <option key={y} value={y}>{y}</option>)}
      </select>
    }>
      <p className="mb-3 text-xs text-zinc-500">Tết Âm lịch và Giỗ tổ Hùng Vương thay đổi hằng năm — hãy thêm theo thông báo chính thức.</p>
      <ErrorText>{error || actionError}</ErrorText>
      {loading && !data ? <Loading /> : (data || []).length === 0 ? <Empty>Chưa có ngày lễ nào trong năm {year}.</Empty> : (
        <ul className="divide-y divide-zinc-800">
          {data.map((h) => (
            <li key={h.date} className="flex items-center justify-between py-2 text-sm">
              <span><span className="mr-3 tabular-nums text-zinc-400">{formatDate(h.date)}</span>{h.name}</span>
              {editable && <button type="button" className="text-zinc-500 hover:text-rose-300" onClick={() => remove(h.date)} aria-label="Xóa"><Trash2 size={15} /></button>}
            </li>
          ))}
        </ul>
      )}
      {editable && (
        <form className="mt-4 grid gap-2 sm:grid-cols-[180px_1fr_auto]" onSubmit={add}>
          <input type="date" className={control} value={form.date} onChange={(e) => setForm({ ...form, date: e.target.value })} required />
          <input className={control} placeholder="Tên ngày lễ" maxLength={150} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
          <button type="submit" className={btnPrimary}><Plus size={15} />Thêm</button>
        </form>
      )}
    </Section>
  );
}

const EMPTY_TYPE = { code: "", name: "", description: "", paid: true, annualQuota: "", requiresDocuments: false, active: true };

function LeaveTypes({ editable }) {
  const { leaveTypes, reload } = useAttendance();
  const [editing, setEditing] = useState(null);
  return (
    <Section title="Loại nghỉ phép" action={editable && <button type="button" className={btnGhost} onClick={() => setEditing(EMPTY_TYPE)}><Plus size={15} />Thêm loại nghỉ</button>}>
      <div className="overflow-x-auto">
        <table className="min-w-full text-sm">
          <thead className="text-left text-xs uppercase text-zinc-400"><tr><th className="py-2 pr-4">Mã</th><th className="py-2 pr-4">Tên</th><th className="py-2 pr-4">Lương</th><th className="py-2 pr-4">Hạn mức / năm</th><th className="py-2 pr-4">Trạng thái</th><th /></tr></thead>
          <tbody className="divide-y divide-zinc-800">
            {leaveTypes.map((t) => (
              <tr key={t.id}>
                <td className="py-2 pr-4 font-mono text-xs">{t.code}</td>
                <td className="py-2 pr-4">{t.name}</td>
                <td className="py-2 pr-4"><Badge tone={t.paid ? "emerald" : "zinc"}>{t.paid ? "Có lương" : "Không lương"}</Badge></td>
                <td className="py-2 pr-4 tabular-nums">{daysText(t.annualQuota)}</td>
                <td className="py-2 pr-4">{t.active ? "Đang dùng" : "Ngừng"}</td>
                <td className="py-2 text-right">{editable && <button type="button" className="text-xs text-amber-300 hover:underline" onClick={() => setEditing({ ...t, annualQuota: t.annualQuota ?? "", description: t.description || "" })}>Sửa</button>}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {editing && <LeaveTypeDialog value={editing} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); reload(); }} />}
    </Section>
  );
}

function LeaveTypeDialog({ value, onClose, onSaved }) {
  const [form, setForm] = useState(value);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    const body = { ...form, annualQuota: form.annualQuota === "" ? null : Number(form.annualQuota) };
    try {
      if (form.id) await attendanceApi.updateLeaveType(form.id, body);
      else await attendanceApi.createLeaveType(body);
      onSaved();
    } catch (e) {
      setError(e.message);
      setSaving(false);
    }
  };
  return (
    <AdminDialog onClose={onClose}>
      <form onSubmit={submit}>
        <AdminDialogHeader title={form.id ? "Sửa loại nghỉ" : "Thêm loại nghỉ"} onClose={onClose} />
        <AdminDialogBody className="space-y-3">
          <div className="grid gap-3 sm:grid-cols-2">
            <Field label="Mã" required><input className={control} value={form.code} maxLength={30} disabled={Boolean(form.id)} onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })} required /></Field>
            <Field label="Tên" required><input className={control} value={form.name} maxLength={100} onChange={(e) => setForm({ ...form, name: e.target.value })} required /></Field>
            <Field label="Hạn mức ngày / năm" hint="Bỏ trống = không giới hạn"><input type="number" min="0" max="366" step="0.5" className={control} value={form.annualQuota} onChange={(e) => setForm({ ...form, annualQuota: e.target.value })} /></Field>
          </div>
          <Field label="Mô tả"><textarea className={textarea} rows={2} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></Field>
          <div className="flex flex-wrap gap-4 text-sm text-zinc-300">
            <label className="flex items-center gap-2"><input type="checkbox" checked={form.paid} onChange={(e) => setForm({ ...form, paid: e.target.checked })} />Hưởng lương</label>
            <label className="flex items-center gap-2"><input type="checkbox" checked={form.requiresDocuments} onChange={(e) => setForm({ ...form, requiresDocuments: e.target.checked })} />Cần giấy tờ</label>
            <label className="flex items-center gap-2"><input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} />Đang sử dụng</label>
          </div>
          <ErrorText>{error}</ErrorText>
        </AdminDialogBody>
        <AdminDialogFooter>
          <button type="button" className={btnGhost} onClick={onClose}>Hủy</button>
          <button type="submit" className={btnPrimary} disabled={saving}>{saving ? "Đang lưu…" : "Lưu"}</button>
        </AdminDialogFooter>
      </form>
    </AdminDialog>
  );
}
