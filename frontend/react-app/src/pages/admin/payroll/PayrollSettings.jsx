import { useState } from "react";
import { Plus } from "lucide-react";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../../components/admin/common/AdminDialog";
import { payrollApi } from "../../../api/admin/payrollApi";
import { Badge, ErrorText, Field, Loading, Section, btnGhost, btnPrimary, control, textarea } from "../../../components/admin/recruitment/RecruitmentUi";
import { usePayroll } from "./PayrollLayout";
import { KIND_LABEL, METHODS, methodLabel, money } from "./payrollLogic";

export default function PayrollSettings() {
  const { can, settings, components, reload } = usePayroll();
  const editable = Boolean(can.globalAdmin);
  const [editing, setEditing] = useState(null);
  if (!settings) return <Loading />;
  return (
    <div className="space-y-5">
      {!editable && <p className="text-sm text-zinc-400">Chỉ quản trị lương cấp chuỗi được thay đổi chính sách. Bạn đang xem ở chế độ chỉ đọc.</p>}
      <PolicyForm key={JSON.stringify(settings)} initial={settings} editable={editable} onSaved={reload} />
      <Section title="Danh mục khoản lương" action={editable && <button type="button" className={btnGhost} onClick={() => setEditing({ code: "", name: "", kind: "ALLOWANCE", calcMethod: "FIXED", defaultAmount: "", defaultRate: "", active: true, note: "", sortOrder: 100 })}><Plus size={15} />Thêm khoản</button>}>
        <div className="overflow-x-auto">
          <table className="min-w-full text-sm">
            <thead className="text-left text-xs uppercase text-zinc-400"><tr><th className="py-2 pr-3">Khoản</th><th className="py-2 pr-3">Loại</th><th className="py-2 pr-3">Cách tính</th><th className="py-2 pr-3 text-right">Mức mặc định</th><th className="py-2 pr-3">Trạng thái</th><th /></tr></thead>
            <tbody className="divide-y divide-zinc-800">
              {components.map((c) => (
                <tr key={c.id}>
                  <td className="py-2 pr-3">{c.name}<div className="font-mono text-xs text-zinc-500">{c.code}</div>{c.note && <div className="text-xs text-zinc-500">{c.note}</div>}</td>
                  <td className="py-2 pr-3">{KIND_LABEL[c.kind]}</td>
                  <td className="py-2 pr-3 text-zinc-300">{methodLabel(c.calcMethod)}</td>
                  <td className="py-2 pr-3 text-right tabular-nums">{c.calcMethod === "PERCENT_OF_BASE" ? `${c.defaultRate ?? 0}%` : c.defaultAmount != null ? money(c.defaultAmount) : "—"}</td>
                  <td className="py-2 pr-3"><Badge tone={c.active ? "emerald" : "zinc"}>{c.active ? "Đang dùng" : "Ngừng"}</Badge></td>
                  <td className="py-2 text-right">{editable && <button type="button" className="text-xs text-amber-300 hover:underline" onClick={() => setEditing({ ...c, defaultAmount: c.defaultAmount ?? "", defaultRate: c.defaultRate ?? "", note: c.note || "" })}>Sửa</button>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <p className="mt-3 text-xs text-zinc-500">BHXH / thuế TNCN chưa tính tự động: gán khoản % lương cơ bản hoặc nhập theo kỳ trên bảng lương.</p>
      </Section>
      {editing && <ComponentDialog value={editing} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); reload(); }} />}
    </div>
  );
}

function PolicyForm({ initial, editable, onSaved }) {
  const [form, setForm] = useState(initial);
  const [message, setMessage] = useState({ error: "", ok: "" });
  const [saving, setSaving] = useState(false);
  const set = (patch) => setForm((f) => ({ ...f, ...patch }));
  const save = async (event) => {
    event.preventDefault();
    setSaving(true);
    setMessage({ error: "", ok: "" });
    try {
      await payrollApi.updateSettings({ ...form, fixedStandardDays: Number(form.fixedStandardDays), otWeekdayRate: Number(form.otWeekdayRate),
        otWeekendRate: Number(form.otWeekendRate), otHolidayRate: Number(form.otHolidayRate), roundingUnit: Number(form.roundingUnit) });
      setMessage({ error: "", ok: "Đã lưu. Áp dụng cho các kỳ lương đồng bộ từ bây giờ." });
      onSaved();
    } catch (e) {
      setMessage({ error: e.message, ok: "" });
    } finally {
      setSaving(false);
    }
  };
  return (
    <Section title="Chính sách tính lương">
      <form onSubmit={save} className="space-y-4">
        <fieldset disabled={!editable} className="grid gap-3 md:grid-cols-4">
          <Field label="Ngày công chuẩn" hint="Theo lịch: số ngày làm việc trong tháng (trừ ngày nghỉ tuần)">
            <select className={control} value={form.standardDaysMode} onChange={(e) => set({ standardDaysMode: e.target.value })}>
              <option value="CALENDAR">Theo lịch làm việc</option><option value="FIXED">Số ngày cố định</option>
            </select>
          </Field>
          <Field label="Số ngày cố định"><input type="number" min="1" max="31" step="0.5" className={control} value={form.fixedStandardDays} disabled={form.standardDaysMode !== "FIXED"} onChange={(e) => set({ fixedStandardDays: e.target.value })} /></Field>
          <Field label="Làm tròn thực nhận">
            <select className={control} value={form.roundingUnit} onChange={(e) => set({ roundingUnit: e.target.value })}>
              {[1, 10, 100, 500, 1000].map((u) => <option key={u} value={u}>{u.toLocaleString("vi-VN")} ₫</option>)}
            </select>
          </Field>
          <Field label="Cách làm tròn">
            <select className={control} value={form.roundingMode} onChange={(e) => set({ roundingMode: e.target.value })}>
              <option value="HALF_UP">Gần nhất</option><option value="DOWN">Xuống</option><option value="UP">Lên</option>
            </select>
          </Field>
          <Field label="Hệ số tăng ca ngày thường"><input type="number" min="1" max="10" step="0.1" className={control} value={form.otWeekdayRate} onChange={(e) => set({ otWeekdayRate: e.target.value })} /></Field>
          <Field label="Hệ số ngày nghỉ tuần"><input type="number" min="1" max="10" step="0.1" className={control} value={form.otWeekendRate} onChange={(e) => set({ otWeekendRate: e.target.value })} /></Field>
          <Field label="Hệ số ngày lễ"><input type="number" min="1" max="10" step="0.1" className={control} value={form.otHolidayRate} onChange={(e) => set({ otHolidayRate: e.target.value })} /></Field>
        </fieldset>
        <p className="text-xs text-zinc-500">Lương theo công = Lương cơ bản / Ngày công chuẩn × Ngày được trả lương. Thực nhận = Lương theo công + Phụ cấp + Thưởng + Tăng ca − Khấu trừ. Ngày nghỉ tuần, ngày lễ và giờ công chuẩn cấu hình ở mục Chấm công.</p>
        <ErrorText>{message.error}</ErrorText>
        {message.ok && <p className="text-sm text-emerald-300">{message.ok}</p>}
        {editable && <div className="flex justify-end"><button type="submit" className={btnPrimary} disabled={saving}>{saving ? "Đang lưu…" : "Lưu chính sách"}</button></div>}
      </form>
    </Section>
  );
}

function ComponentDialog({ value, onClose, onSaved }) {
  const [form, setForm] = useState(value);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const set = (patch) => setForm((f) => ({ ...f, ...patch }));
  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    const body = { ...form, defaultAmount: form.defaultAmount === "" ? null : Number(form.defaultAmount), defaultRate: form.defaultRate === "" ? null : Number(form.defaultRate),
      sortOrder: Number(form.sortOrder) || 100, note: form.note.trim() || null };
    try {
      if (form.id) await payrollApi.updateComponent(form.id, body);
      else await payrollApi.createComponent(body);
      onSaved();
    } catch (e) {
      setError(e.message);
      setSaving(false);
    }
  };
  return (
    <AdminDialog onClose={onClose}>
      <form onSubmit={submit}>
        <AdminDialogHeader title={form.id ? "Sửa khoản lương" : "Thêm khoản lương"} description={form.id ? "Mã, loại và cách tính không đổi được sau khi tạo." : undefined} onClose={onClose} />
        <AdminDialogBody className="space-y-3">
          <div className="grid gap-3 sm:grid-cols-2">
            <Field label="Mã" required><input className={control} maxLength={40} pattern="[A-Za-z0-9_]+" value={form.code} disabled={Boolean(form.id)} onChange={(e) => set({ code: e.target.value.toUpperCase() })} required /></Field>
            <Field label="Tên" required><input className={control} maxLength={150} value={form.name} onChange={(e) => set({ name: e.target.value })} required /></Field>
            <Field label="Loại">
              <select className={control} value={form.kind} disabled={Boolean(form.id)} onChange={(e) => set({ kind: e.target.value })}>
                <option value="ALLOWANCE">Phụ cấp</option><option value="BONUS">Thưởng</option><option value="DEDUCTION">Khấu trừ</option>
              </select>
            </Field>
            <Field label="Cách tính">
              <select className={control} value={form.calcMethod} disabled={Boolean(form.id)} onChange={(e) => set({ calcMethod: e.target.value })}>
                {METHODS.map((m) => <option key={m.value} value={m.value}>{m.label}</option>)}
              </select>
            </Field>
            {form.calcMethod === "PERCENT_OF_BASE"
              ? <Field label="Tỷ lệ mặc định (%)" required><input type="number" min="0" max="100" step="0.01" className={control} value={form.defaultRate} onChange={(e) => set({ defaultRate: e.target.value })} required /></Field>
              : <Field label="Mức mặc định (₫)"><input type="number" min="0" step="1000" className={control} value={form.defaultAmount} onChange={(e) => set({ defaultAmount: e.target.value })} /></Field>}
            <Field label="Thứ tự hiển thị"><input type="number" className={control} value={form.sortOrder} onChange={(e) => set({ sortOrder: e.target.value })} /></Field>
          </div>
          <Field label="Ghi chú / điều kiện áp dụng"><textarea className={textarea} rows={2} maxLength={1000} value={form.note} onChange={(e) => set({ note: e.target.value })} /></Field>
          <label className="flex items-center gap-2 text-sm text-zinc-300"><input type="checkbox" checked={form.active} onChange={(e) => set({ active: e.target.checked })} />Đang sử dụng</label>
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
