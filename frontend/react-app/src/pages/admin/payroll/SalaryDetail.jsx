import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, Lock, Plus, Trash2 } from "lucide-react";
import AdminDialog, { AdminDialogBody, AdminDialogFooter, AdminDialogHeader } from "../../../components/admin/common/AdminDialog";
import { payrollApi } from "../../../api/admin/payrollApi";
import { Badge, Empty, ErrorText, Field, Loading, Section, btnGhost, btnPrimary, control, textarea, useLoader } from "../../../components/admin/recruitment/RecruitmentUi";
import { formatDate, formatDateTime, isoDate } from "../attendance/attendanceLogic";
import { usePayroll } from "./PayrollLayout";
import { ACTION_LABEL, KIND_LABEL, methodLabel, money } from "./payrollLogic";

export default function SalaryDetail() {
  const { userId } = useParams();
  const { can } = usePayroll();
  const { data, error, loading, reload } = useLoader((signal) => payrollApi.salary(userId, { signal }), [userId]);
  const [editing, setEditing] = useState(null);

  if (loading && !data) return <Loading />;
  if (error) return <div className="space-y-3"><Back /><ErrorText>{error}</ErrorText></div>;
  const { employee, structures, history } = data;
  const latest = structures[0];

  return (
    <div className="space-y-5">
      <Back />
      <Section title={employee.fullName} action={can.salaryManage && (
        <button type="button" className={btnPrimary} onClick={() => setEditing({ mode: "new", base: latest })}><Plus size={15} />{latest ? "Thay đổi lương (phiên bản mới)" : "Tạo hồ sơ lương"}</button>
      )}>
        <dl className="grid gap-3 text-sm sm:grid-cols-4">
          <Info label="Mã nhân viên" value={employee.employeeCode} />
          <Info label="Cửa hàng / phòng ban" value={`${employee.storeName || "Văn phòng"}${employee.departmentName ? ` · ${employee.departmentName}` : ""}`} />
          <Info label="Chức vụ" value={employee.positionName || "—"} />
          <Info label="Ngày vào làm / nghỉ" value={`${formatDate(employee.hireDate) || "—"}${employee.terminationDate ? ` → ${formatDate(employee.terminationDate)}` : ""}`} />
        </dl>
      </Section>

      <Section title="Các phiên bản lương">
        {structures.length === 0 ? <Empty>Nhân viên chưa có hồ sơ lương — sẽ không được đưa vào bảng lương.</Empty> : (
          <div className="space-y-3">
            {structures.map((s) => (
              <div key={s.id} className="rounded-2xl border border-zinc-800 p-4">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div>
                    <span className="text-lg font-semibold tabular-nums">{money(s.baseSalary)}</span>
                    <span className="ml-2 text-sm text-zinc-400">từ {formatDate(s.effectiveFrom)}{s.effectiveTo ? ` đến ${formatDate(s.effectiveTo)}` : " (hiện hành)"}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    {s.confidential && <Badge tone="violet">Bảo mật</Badge>}
                    {s.locked ? <span className="inline-flex items-center gap-1 text-xs text-zinc-500"><Lock size={13} />Đã dùng cho kỳ lương đã chốt</span>
                      : can.salaryManage && <button type="button" className="text-xs text-amber-300 hover:underline" onClick={() => setEditing({ mode: "edit", base: s })}>Sửa</button>}
                  </div>
                </div>
                <p className="mt-1 text-xs text-zinc-500">
                  {s.payMethod === "CASH" ? "Tiền mặt" : `Chuyển khoản ${s.bankName || ""} ${s.bankAccount || ""}`} · Tăng ca: {s.overtimeEligible ? (s.overtimeHourlyRate ? `${money(s.overtimeHourlyRate)}/giờ` : "theo lương cơ bản") : "không áp dụng"}
                  {s.note && ` · ${s.note}`}
                </p>
                {s.items.length > 0 && (
                  <ul className="mt-2 flex flex-wrap gap-2 text-xs">
                    {s.items.map((i) => (
                      <li key={i.componentId} className="rounded-full border border-zinc-700 px-2.5 py-1">
                        {KIND_LABEL[i.kind]}: {i.name} — {i.calcMethod === "PERCENT_OF_BASE" ? `${i.rate}%` : money(i.amount)} <span className="text-zinc-500">({methodLabel(i.calcMethod)})</span>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            ))}
          </div>
        )}
      </Section>

      <Section title="Lịch sử thay đổi lương">
        {history.length === 0 ? <Empty>Chưa có thay đổi.</Empty> : (
          <ul className="space-y-2 text-sm">
            {history.map((h, i) => (
              <li key={i} className="rounded-xl border border-zinc-800 px-3 py-2">
                <div className="flex flex-wrap justify-between gap-2"><b>{ACTION_LABEL[h.action] || h.action}</b><span className="text-xs text-zinc-500">{h.changedByName} · {formatDateTime(h.changedAt)}</span></div>
                <p className="text-zinc-400">{h.reason}</p>
                {(h.oldData?.baseSalary != null || h.newData?.baseSalary != null) && (
                  <p className="text-xs text-zinc-500">Lương cơ bản: {h.oldData?.baseSalary != null ? money(h.oldData.baseSalary) : "—"} → {h.newData?.baseSalary != null ? money(h.newData.baseSalary) : "—"}</p>
                )}
                {h.action === "CLOSE" && <p className="text-xs text-zinc-500">Kết thúc hiệu lực: {formatDate(h.newData?.effectiveTo)}</p>}
              </li>
            ))}
          </ul>
        )}
      </Section>

      {editing && <StructureDialog userId={userId} mode={editing.mode} base={editing.base} onClose={() => setEditing(null)} onSaved={() => { setEditing(null); reload(); }} />}
    </div>
  );
}

function Back() {
  return <Link to="/admin/payroll/salaries" className="inline-flex items-center gap-1 text-sm text-zinc-400 hover:text-white"><ArrowLeft size={14} />Hồ sơ lương</Link>;
}

function Info({ label, value }) {
  return <div><dt className="text-xs text-zinc-500">{label}</dt><dd className="mt-0.5 text-zinc-200">{value}</dd></div>;
}

function nextMonthStart() {
  const d = new Date();
  return isoDate(new Date(d.getFullYear(), d.getMonth() + 1, 1));
}

function StructureDialog({ userId, mode, base, onClose, onSaved }) {
  const { components, can } = usePayroll();
  const editing = mode === "edit";
  const [form, setForm] = useState(() => ({
    baseSalary: base?.baseSalary ?? "",
    effectiveFrom: editing ? base.effectiveFrom : base ? nextMonthStart() : isoDate(),
    effectiveTo: editing ? base.effectiveTo || "" : "",
    payMethod: base?.payMethod || "BANK_TRANSFER",
    bankName: base?.bankName || "", bankAccount: base?.bankAccount || "",
    overtimeEligible: base ? base.overtimeEligible : true, overtimeHourlyRate: base?.overtimeHourlyRate ?? "",
    confidential: base?.confidential || false, note: base?.note || "", reason: "",
    items: (base?.items || []).map((i) => ({ componentId: i.componentId, amount: i.amount ?? "", rate: i.rate ?? "", note: i.note || "" })),
  }));
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const active = components.filter((c) => c.active && c.calcMethod !== "MANUAL");
  const set = (patch) => setForm((f) => ({ ...f, ...patch }));
  const setItem = (index, patch) => set({ items: form.items.map((it, i) => (i === index ? { ...it, ...patch } : it)) });
  const addItem = () => {
    const c = active.find((x) => !form.items.some((i) => i.componentId === x.id));
    if (c) set({ items: [...form.items, { componentId: c.id, amount: c.defaultAmount ?? "", rate: c.defaultRate ?? "", note: "" }] });
  };

  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");
    const body = {
      ...form, baseSalary: Number(form.baseSalary), effectiveTo: form.effectiveTo || null,
      overtimeHourlyRate: form.overtimeHourlyRate === "" ? null : Number(form.overtimeHourlyRate),
      bankName: form.bankName.trim() || null, bankAccount: form.bankAccount.trim() || null, note: form.note.trim() || null, reason: form.reason.trim(),
      items: form.items.map((i) => ({ componentId: i.componentId, amount: i.amount === "" ? null : Number(i.amount), rate: i.rate === "" ? null : Number(i.rate), note: i.note.trim() || null })),
    };
    try {
      if (editing) await payrollApi.updateStructure(base.id, body);
      else await payrollApi.createStructure(userId, body);
      onSaved();
    } catch (e) {
      setError(e.message);
      setSaving(false);
    }
  };

  return (
    <AdminDialog onClose={onClose} size="lg">
      <form onSubmit={submit}>
        <AdminDialogHeader title={editing ? "Sửa phiên bản lương" : base ? "Thay đổi lương" : "Tạo hồ sơ lương"}
          description={editing ? "Chỉ sửa được phiên bản chưa dùng cho kỳ lương đã gửi duyệt / chốt." : base ? "Tạo phiên bản mới có hiệu lực từ ngày chọn; mức lương cũ được giữ nguyên trong lịch sử." : undefined} onClose={onClose} />
        <AdminDialogBody className="space-y-4">
          <div className="grid gap-3 sm:grid-cols-3">
            <Field label="Lương cơ bản / tháng (₫)" required><input type="number" min="1" step="1" className={control} value={form.baseSalary} onChange={(e) => set({ baseSalary: e.target.value })} required /></Field>
            <Field label="Hiệu lực từ" required><input type="date" className={control} value={form.effectiveFrom} onChange={(e) => set({ effectiveFrom: e.target.value })} required /></Field>
            <Field label="Hiệu lực đến" hint="Bỏ trống nếu không thời hạn"><input type="date" className={control} min={form.effectiveFrom} value={form.effectiveTo} onChange={(e) => set({ effectiveTo: e.target.value })} /></Field>
            <Field label="Hình thức trả lương">
              <select className={control} value={form.payMethod} onChange={(e) => set({ payMethod: e.target.value })}>
                <option value="BANK_TRANSFER">Chuyển khoản</option><option value="CASH">Tiền mặt</option>
              </select>
            </Field>
            {form.payMethod === "BANK_TRANSFER" && <>
              <Field label="Ngân hàng"><input className={control} maxLength={100} value={form.bankName} onChange={(e) => set({ bankName: e.target.value })} /></Field>
              <Field label="Số tài khoản" required><input className={control} maxLength={50} value={form.bankAccount} onChange={(e) => set({ bankAccount: e.target.value })} required /></Field>
            </>}
            <Field label="Đơn giá tăng ca (₫/giờ)" hint="Bỏ trống = lương CB / công chuẩn / giờ công"><input type="number" min="1" className={control} value={form.overtimeHourlyRate} disabled={!form.overtimeEligible} onChange={(e) => set({ overtimeHourlyRate: e.target.value })} /></Field>
          </div>
          <div className="flex flex-wrap gap-4 text-sm text-zinc-300">
            <label className="flex items-center gap-2"><input type="checkbox" checked={form.overtimeEligible} onChange={(e) => set({ overtimeEligible: e.target.checked })} />Được hưởng tăng ca</label>
            {can.admin && <label className="flex items-center gap-2"><input type="checkbox" checked={form.confidential} onChange={(e) => set({ confidential: e.target.checked })} />Hồ sơ bảo mật (chỉ quản trị lương xem)</label>}
          </div>

          <div>
            <div className="mb-2 flex items-center justify-between"><h3 className="text-sm font-semibold text-zinc-200">Phụ cấp, thưởng, khấu trừ cố định / theo điều kiện</h3>
              <button type="button" className={btnGhost} onClick={addItem} disabled={form.items.length >= active.length}><Plus size={14} />Thêm khoản</button></div>
            {form.items.length === 0 ? <p className="text-sm text-zinc-500">Chưa có khoản nào. Thưởng / khấu trừ phát sinh từng tháng nhập trực tiếp trên bảng lương.</p> : (
              <div className="space-y-2">
                {form.items.map((item, index) => {
                  const c = components.find((x) => x.id === item.componentId);
                  const percent = c?.calcMethod === "PERCENT_OF_BASE";
                  return (
                    <div key={index} className="grid items-end gap-2 sm:grid-cols-[1.4fr_1fr_1fr_auto]">
                      <Field label="Khoản">
                        <select className={control} value={item.componentId} onChange={(e) => {
                          const next = components.find((x) => x.id === e.target.value);
                          setItem(index, { componentId: e.target.value, amount: next?.defaultAmount ?? "", rate: next?.defaultRate ?? "" });
                        }}>
                          {active.filter((x) => x.id === item.componentId || !form.items.some((i) => i.componentId === x.id)).map((x) => <option key={x.id} value={x.id}>{KIND_LABEL[x.kind]}: {x.name}</option>)}
                        </select>
                      </Field>
                      <Field label={percent ? "Tỷ lệ (%)" : "Số tiền (₫)"} hint={methodLabel(c?.calcMethod)}>
                        {percent ? <input type="number" min="0" max="100" step="0.01" className={control} value={item.rate} onChange={(e) => setItem(index, { rate: e.target.value })} required />
                          : <input type="number" min="0" step="1000" className={control} value={item.amount} onChange={(e) => setItem(index, { amount: e.target.value })} required />}
                      </Field>
                      <Field label="Ghi chú"><input className={control} maxLength={500} value={item.note} onChange={(e) => setItem(index, { note: e.target.value })} /></Field>
                      <button type="button" className="mb-2 text-zinc-500 hover:text-rose-300" aria-label="Bỏ" onClick={() => set({ items: form.items.filter((_, i) => i !== index) })}><Trash2 size={15} /></button>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
          <Field label="Ghi chú hồ sơ"><textarea className={textarea} rows={2} maxLength={1000} value={form.note} onChange={(e) => set({ note: e.target.value })} /></Field>
          <Field label="Lý do thay đổi (lưu lịch sử)" required><input className={control} maxLength={1000} value={form.reason} onChange={(e) => set({ reason: e.target.value })} required /></Field>
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
